
description = "Kotlin Preloader"

apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(ideaSdkDeps("asm-all"))
    buildVersion()
}

sourceSets {
    "main" {
        java {
            srcDirs( "src", "instrumentation/src")
        }
    }
    "test" { none() }
}

runtimeJar {
    manifest.attributes.put("Main-Class", "org.jetbrains.kotlin.preloading.Preloader")
}

dist()
