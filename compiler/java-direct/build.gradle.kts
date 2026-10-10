
description = "Kotlin Java Direct Compiler Plugin"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("test-inputs-check")
}

dependencies {
    api(project(":core:compiler.common.jvm"))

    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)
    implementation(project(":compiler:frontend.common"))
    implementation(project(":compiler:frontend.common.jvm"))
    implementation(project(":compiler:plugin-api"))
    implementation(project(":compiler:fir:resolve"))
    implementation(project(":compiler:fir:fir-jvm"))
    implementation(project(":compiler:multiplatform-parsing"))

    testImplementation(intellijCore())
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.intellij.fastutil)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

optInToExperimentalCompilerApi()

projectTests {
    testTask()
}
