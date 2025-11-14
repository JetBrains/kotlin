import org.jetbrains.kotlin.build.foreign.CheckForeignClassUsageTask

plugins {
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("kotlin-git.gradle-build-conventions.foreign-class-usage-checker")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    api(kotlinStdlib())
    implementation(project(":core:util.runtime"))
    implementation(project(":compiler:compiler.version"))
    implementation(libs.intellij.asm)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val checkForeignClassUsage by tasks.registering(CheckForeignClassUsageTask::class) {
    outputFile = file("api/psi-utils-api.foreign")
}