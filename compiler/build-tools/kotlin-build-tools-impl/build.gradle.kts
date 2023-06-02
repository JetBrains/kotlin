plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:build-tools:kotlin-build-tools-api"))
    implementation(kotlinStdlib())
}

publish()

standardPublicJars()

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
    }
}