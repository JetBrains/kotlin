plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(kotlinStdlib())
    compileOnly(project(":kotlin-build-common")) // not sure
}

publish()