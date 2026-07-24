import org.jetbrains.kotlin.build.foreign.CheckForeignClassUsageTask
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
    id("kotlin-git.gradle-build-conventions.foreign-class-usage-checker")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    api(kotlinStdlib())
    api(project(":core:language.model"))
    implementation(project(":compiler:compiler.version"))
    testImplementation(kotlin("test-junit5"))
}

kotlin {
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        /**
         * Binary compatibility is expected for all public API, except for the `LanguageFeature` enum itself.
         * The enum is modified very frequently, so and is excluded from binary compatibility guarantees
         * to avoid requiring contributors to update the dump file on every change.
         */
        filters.exclude.byNames.add("org.jetbrains.kotlin.config.LanguageFeature")
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTests {
    testTask(javaLauncher = JdkMajorVersion.JDK_1_8)
}

val checkForeignClassUsage = tasks.register("checkForeignClassUsage", CheckForeignClassUsageTask::class) {
    outputFile = file("api/language-version-settings-api.foreign")
}
