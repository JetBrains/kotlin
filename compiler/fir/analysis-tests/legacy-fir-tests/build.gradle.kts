/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    testFixturesApi(libs.junit4)
    testFixturesCompileOnly(kotlinTest("junit"))
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(project(":compiler:fir:checkers"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.jvm"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.js"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.native"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.wasm"))
    testFixturesApi(project(":compiler:fir:entrypoint"))
    testFixturesApi(project(":compiler:frontend"))

    testFixturesApi(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testRuntimeOnly(project(":core:descriptors.runtime"))

    testFixturesCompileOnly(intellijCore())
    testRuntimeOnly(intellijCore())
}

optInToK1Deprecation()

sourceSets {
    "main" { none() }
    "testFixtures" { projectDefault() }
}

projectTests {
    testTask(parallel = true, maxHeapSizeMb = 3072, jUnitMode = JUnitMode.JUnit4) {
        dependsOn(":dist")
    }

    testGenerator("org.jetbrains.kotlin.fir.TestGeneratorForLegacyFirTestsKt", generateTestsInBuildDirectory = true)

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withMockJdkRuntime()
    withMockJdkAnnotationsJar()
    withAnnotations()
    withThirdPartyAnnotations()
    withThirdPartyJsr305()

    testData(project(":compiler").isolated, "testData/loadJava/compiledJava")
    testData(project(":compiler:fir:analysis-tests").isolated, "testData/enhancement")
    testData(project(":compiler:fir:analysis-tests").isolated, "testData/lightClasses")
}

testsJar()
