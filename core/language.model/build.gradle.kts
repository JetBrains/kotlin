import org.jetbrains.kotlin.build.foreign.CheckForeignClassUsageTask
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
    id("kotlin-git.gradle-build-conventions.foreign-class-usage-checker")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    api(kotlinStdlib())
}

kotlin {
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation()
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

val checkForeignClassUsage = tasks.register("checkForeignClassUsage", CheckForeignClassUsageTask::class) {
    outputFile = file("api/language-model-api.foreign")
}
