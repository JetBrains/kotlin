import org.jetbrains.kotlin.build.foreign.CheckForeignClassUsageTask
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
    id("kotlin-git.gradle-build-conventions.foreign-class-usage-checker")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    api(kotlinStdlib())
    api(project(":core:language.targets"))
    implementation(libs.intellij.asm)
}

kotlin {
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation()
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

val checkForeignClassUsage by tasks.registering(CheckForeignClassUsageTask::class) {
    outputFile = file("api/language-targets-jvm-api.foreign")
}