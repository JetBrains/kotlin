import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jmailen.gradle.kotlinter.KotlinterPlugin
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

plugins {
  kotlin("jvm") version "1.8.20" apply false
  id("org.jetbrains.dokka") version "1.8.10" apply false
  id("com.gradle.plugin-publish") version "1.1.0" apply false
  id("com.github.gmazzo.buildconfig") version "3.1.0" apply false
  id("org.jmailen.kotlinter") version "3.14.0" apply false
}

allprojects {
  group = "com.bnorm.power"
  version = "0.14.0-SNAPSHOT"
}

allprojects {
  repositories {
    mavenCentral()
  }

  tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
  }

  tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
  }

  plugins.withType<KotlinterPlugin> {
    val formatBuildscripts = tasks.register<FormatTask>("formatBuildscripts") {
      group = "verification"
      source(layout.projectDirectory.asFileTree.matching { include("**.kts") })
    }
    tasks.named("formatKotlin") { dependsOn(formatBuildscripts) }

    val lintBuildscripts = tasks.register<LintTask>("lintBuildscripts") {
      group = "verification"
      source(layout.projectDirectory.asFileTree.matching { include("**.kts") })
    }
    tasks.named("lintKotlin") { dependsOn(lintBuildscripts) }
  }
}
