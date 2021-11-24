plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:fir:raw-fir:psi2fir"))
    api(project(":compiler:fir:resolve"))
    api(project(":compiler:visualizer:common"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions {
        freeCompilerArgs += "-opt-in=org.jetbrains.kotlin.fir.symbols.SymbolInternals"
    }
}
