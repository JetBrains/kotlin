plugins {
    kotlin("jvm")
    id("jps-compatible")
}

configureKotlinCompileTasksGradleCompatibility()

dependencies {
    implementation(kotlinStdlib())
    implementation(project(":incremental-compilation-facade-api"))
    compileOnly(project(":compiler:incremental-compilation-impl"))
}

publish()