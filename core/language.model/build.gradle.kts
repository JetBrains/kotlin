import org.jetbrains.kotlin.build.foreign.registerForeignClassUsageTasks
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
    id("kotlin-git.gradle-build-conventions.foreign-class-usage-checker")
}

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

registerForeignClassUsageTasks {
    outputFile = file("api/language-model-api.foreign")
}
