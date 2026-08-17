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

// Shaded into `kotlin-reflect`, whose `dexMethodCount` dexes the jar with a D8 that cannot read the
// `MethodParameters` attribute a modern `javac` emits for bridge methods even under `--release 8`.
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
    outputFile = file("api/names-api.foreign")
}
