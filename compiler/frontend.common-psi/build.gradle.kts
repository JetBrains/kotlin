plugins {
    kotlin("jvm")
    id("java-instrumentation")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:frontend.common"))
    api(project(":compiler:psi"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
