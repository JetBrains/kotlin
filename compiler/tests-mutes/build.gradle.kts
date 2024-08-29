plugins {
    kotlin("jvm")
    id("java-instrumentation")
    id("jps-compatible")
}

dependencies {
    api(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
