plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

configureKotlinCompileTasksGradleCompatibility()

dependencies {
    compileOnly(kotlinStdlib())
}

kotlin {
    explicitApi()
}

publish()

standardPublicJars()