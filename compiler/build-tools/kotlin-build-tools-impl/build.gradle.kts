plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:build-tools:kotlin-build-tools-api"))
    implementation(kotlinStdlib())
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:cli-js"))
    runtimeOnly(project(":kotlin-compiler-embeddable"))
}

publish()

standardPublicJars()

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
    }
}