
plugins {
    kotlin("jvm")
    id("java-instrumentation")
    id("jps-compatible")
}

dependencies {
    api(project(":core:descriptors"))
    api(project(":compiler:util"))
    api(project(":compiler:frontend"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
