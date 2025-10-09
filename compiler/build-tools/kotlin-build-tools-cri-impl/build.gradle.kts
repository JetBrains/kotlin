plugins {
    kotlin("jvm")
    id("project-tests-convention")
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":compiler:build-tools:kotlin-build-tools-api"))
    implementation(kotlinStdlib())

    implementation(libs.kotlinx.serialization.protobuf)

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

publish()

runtimeJar()
sourcesJar()
javadocJar()

kotlin {
    explicitApi()
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
    }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5)
}
