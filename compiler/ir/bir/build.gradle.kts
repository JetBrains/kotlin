plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:bir:tree"))
    api(project(":compiler:bir:backend"))

    api(project(":compiler:backend"))
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:backend.common.jvm"))
    compileOnly(intellijCore())
}

optInToIrSymbolInternals()

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions {
        allWarningsAsErrors = false
    }
}