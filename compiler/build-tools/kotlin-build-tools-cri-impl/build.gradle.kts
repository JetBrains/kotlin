plugins {
    kotlin("jvm")
    id("project-tests-convention")
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":compiler:build-tools:kotlin-build-tools-api"))
    implementation(kotlinStdlib())
    compileOnly(project(":kotlin-build-common"))
    compileOnly(project(":core:compiler.common"))

    implementation(libs.kotlinx.serialization.protobuf)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(project(":kotlin-build-common"))
    testImplementation(project(":core:compiler.common"))
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
