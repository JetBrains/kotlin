plugins {
  kotlin("jvm") version "1.3.60" apply false
  id("org.jetbrains.dokka") version "0.10.0" apply false
  id("com.gradle.plugin-publish") version "0.10.1" apply false
}

allprojects {
  group = "com.bnorm.power"
  version = "0.3.0-SNAPSHOT"
}

subprojects {
  repositories {
    mavenCentral()
    jcenter()
  }
}
