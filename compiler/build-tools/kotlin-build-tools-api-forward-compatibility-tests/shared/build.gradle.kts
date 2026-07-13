plugins {
    id("common-configuration")
    id("test-federation-convention")
    kotlin("jvm")
}

dependencies {
    api(kotlinStdlib("jdk8"))
    api(platform(libs.junit.bom))
    compileOnly(libs.junit.jupiter.engine)
    compileOnly(libs.junit.jupiter.params)
    compileOnly("org.jetbrains.kotlin:kotlin-build-tools-api:2.3.0")
}

sourceSets {
    "main" { projectDefault() }
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
    }
}
