
description = "Kotlin Preloader"

apply { plugin("kotlin") }

jvmTarget = "1.6"

configureIntellijPlugin()

afterEvaluate {
    dependencies {
        compileOnly(intellij { include("asm-all.jar") })
    }
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
