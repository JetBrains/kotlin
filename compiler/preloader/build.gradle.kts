
description = "Kotlin Preloader"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))
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
