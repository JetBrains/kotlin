plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":incremental-compilation-facade-api"))
    implementation(kotlinStdlib())
}

publish()