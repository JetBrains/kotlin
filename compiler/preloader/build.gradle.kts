
description = "Kotlin Preloader"

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(libs.intellij.asm)
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
