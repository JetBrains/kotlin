/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
}

dependencies {
    api(project(":compiler:fir:raw-fir:raw-fir.common"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:psi:psi-impl"))
    implementation(kotlinxCollectionsImmutable())

    compileOnly(intellijCore())
    compileOnly(libs.guava)

    testFixturesApi(libs.junit4)
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    testCompileOnly(kotlinTest("junit"))

    testRuntimeOnly(project(":core:descriptors.runtime"))

    testFixturesCompileOnly(intellijCore())
    testImplementation(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit4) {
    workingDir = rootDir
}

testsJar()
