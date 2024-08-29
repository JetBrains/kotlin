plugins {
    kotlin("jvm")
    id("java-instrumentation")
    id("jps-compatible")
}

dependencies {
    api(project(":core:compiler.common"))
    api(project(":compiler:util"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
