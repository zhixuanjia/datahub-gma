package com.linkedin.metadata.dao.utils;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import io.ebean.EbeanServer;
import io.ebean.EbeanServerFactory;
import io.ebean.config.ServerConfig;
import io.ebean.datasource.DataSourceConfig;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import javax.annotation.Nonnull;


/**
 * Create a connection to mysql dev instance.
 */
public class MysqlDevInstance {
  public static final ServerConfig SERVER_CONFIG = createLocalMySQLServerConfig();
  private static volatile DB db;
  private MysqlDevInstance() {
  }

  public static final String DB_SCHEMA = "gma_dev";
  private static final String DB_USER = "user";
  private static final String DB_PASS = "password";
  private static final int PORT = 23306;

  public static EbeanServer getServer() {
    return EbeanServerFactory.create(createLocalMySQLServerConfig());
  }

  @Nonnull
  private static ServerConfig createLocalMySQLServerConfig() {
    initDB();
    DataSourceConfig dataSourceConfig = new DataSourceConfig();
    dataSourceConfig.setUsername(DB_USER);
    dataSourceConfig.setPassword(DB_PASS);
    dataSourceConfig.setUrl(String.format("jdbc:mysql://localhost:%s/%s?allowMultiQueries=true", PORT, DB_SCHEMA));
    dataSourceConfig.setDriver("com.mysql.cj.jdbc.Driver");

    ServerConfig serverConfig = new ServerConfig();
    serverConfig.setName("gma");
    serverConfig.setDataSourceConfig(dataSourceConfig);
    serverConfig.setDdlGenerate(false);
    serverConfig.setDdlRun(false);
    return serverConfig;
  }

  private static void initDB() {
    if (db == null) {
      synchronized (DB.class) {
        if (db == null) { // double check
          DBConfigurationBuilder configurationBuilder = DBConfigurationBuilder.newBuilder();
          configurationBuilder.setPort(PORT);
          String baseDbDir = getBaseDbDir();
          configurationBuilder.setDataDir(baseDbDir + File.separator + "data");
          configurationBuilder.setBaseDir(baseDbDir + File.separator + "base");

          try {
            // ensure the DB directory is deleted before we start to have a clean start
            if (new File(baseDbDir).exists()) {
              Files.walk(Paths.get(baseDbDir)).map(Path::toFile)
                  .sorted(Comparator.reverseOrder())
                  .forEach(File::delete);
            }
            db = DB.newEmbeddedDB(configurationBuilder.build());
            db.start();
            db.createDB(DB_SCHEMA);
          } catch (ManagedProcessException | IOException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
  }

  private static String getBaseDbDir() {
    return String.join(File.separator, System.getProperty("java.io.tmpdir"), "datahub-gma", "testDb");
  }
}
