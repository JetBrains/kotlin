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
    api(project(":core:language.targets"))
    api(project(":core:language.targets.jvm"))
    api(project(":core:language.targets.js"))
    api(project(":core:language.targets.wasm"))
    api(project(":core:language.targets.native"))
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
    outputFile = file("api/language-targets-common-api.foreign")
}
