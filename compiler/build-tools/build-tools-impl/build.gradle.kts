plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:build-tools:build-tools-api"))
    implementation(kotlinStdlib())
}

publish()

standardPublicJars()