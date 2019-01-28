
description = "Kotlin Preloader"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(intellijDep()) { includeJars("asm-all", rootProject = rootProject) }
}

sourceSets {
    "main" {
        java {
            srcDirs( "src", "instrumentation/src")
        }
    }
    "test" {}
}

runtimeJar {
    manifest.attributes.put("Main-Class", "org.jetbrains.kotlin.preloading.Preloader")
}

dist()
