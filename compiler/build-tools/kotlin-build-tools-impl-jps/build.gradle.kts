plugins {
    id("common-configuration")
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:build-tools:kotlin-build-tools-api-jps"))
    implementation(kotlinStdlib())
    compileOnly(project(":core:compiler.common"))
    compileOnly(project(":compiler:util"))
}

publish()

standardPublicJars()

kotlin {
    explicitApi()
    compilerOptions {
        optIn.addAll(
            "org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi",
            "org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi",
        )
    }
}
