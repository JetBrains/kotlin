
description = "Kotlin Preloader"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

sourceSets {
    "main" {
        java {
            srcDirs("src")
        }
    }
    "test" {}
}

runtimeJar {
    manifest.attributes.put("Main-Class", "org.jetbrains.kotlin.preloading.Preloader")
}
