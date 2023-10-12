plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:backend-common"))
    api(project(":compiler:bir:tree"))
    api(project(":compiler:backend.wasm"))
    compileOnly(intellijCore())
}

optInToIrSymbolInternals()

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions {
        allWarningsAsErrors = false
    }
}