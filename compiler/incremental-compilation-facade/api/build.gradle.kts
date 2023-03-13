plugins {
    kotlin("jvm")
    id("jps-compatible")
}

configureKotlinCompileTasksGradleCompatibility()

dependencies {
    implementation(kotlinStdlib())
    compileOnly(project(":kotlin-build-common")) // not sure
}

publish()