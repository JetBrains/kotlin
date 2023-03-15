plugins {
    kotlin("jvm")
    id("jps-compatible")
}

configureKotlinCompileTasksGradleCompatibility()

dependencies {
    implementation(kotlinStdlib())
}

publish()