import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

configureKotlinCompileTasksGradleCompatibility()

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_1_8)
        @Suppress("DEPRECATION")
        apiVersion.set(KotlinVersion.KOTLIN_1_7)
    }
}

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

tasks.compileKotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
    }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    useJUnitPlatform()
}