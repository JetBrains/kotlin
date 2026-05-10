import JdkMajorVersion.JDK_1_8
import JdkMajorVersion.JDK_21_0
import TestCompilePaths.KOTLIN_STDLIB_SOURCES_ROOT_PATH

/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    testFixturesApi(libs.junit4)
    testFixturesCompileOnly(kotlinTest("junit"))
    testImplementation(libs.junit.jupiter.api)

    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(project(":compiler:fir:checkers"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.jvm"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.js"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.native"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.wasm"))
    testFixturesApi(project(":compiler:fir:entrypoint"))

    testFixturesApi(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    testFixturesCompileOnly(intellijCore())
    testImplementation(intellijCore())

    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

optInToK1Deprecation()

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

projectTests {
    testTask(maxHeapSizeMb = 3072, jUnitMode = JUnitMode.JUnit5, defineJDKEnvVariables = listOf(JDK_1_8, JDK_21_0))

    testGenerator("org.jetbrains.kotlin.fir.TestGeneratorForLegacyFirTestsKt", generateTestsInBuildDirectory = true)

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withMockJdkRuntime()
    withMockJdkAnnotationsJar()
    withMockJDKModifiedRuntime()
    withAnnotations()
    withThirdPartyAnnotations()
    withThirdPartyJsr305()

    testData(project(":compiler").isolated, "testData/loadJava/compiledJava")
    testData(project.isolated, "testData")
}

testsJar()
