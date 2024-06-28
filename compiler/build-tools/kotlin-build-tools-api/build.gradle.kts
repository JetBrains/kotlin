import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

configureKotlinCompileTasksGradleCompatibility()

dependencies {
    compileOnly(kotlinStdlib())
    compileOnly(project(":compiler:build-tools:kotlin-build-tools-jdk-utils"))
    embedded(project(":compiler:build-tools:kotlin-build-tools-jdk-utils"))
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(kotlinStdlib())
    testImplementation(project(":compiler:build-tools:kotlin-build-tools-jdk-utils"))
}

kotlin {
    explicitApi()
}

publish()

standardPublicJars()

tasks.named<KotlinCompile>("compileTestKotlin") {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
    }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    useJUnitPlatform()
}