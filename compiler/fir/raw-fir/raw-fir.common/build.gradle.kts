/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("generated-sources")
}

dependencies {
    api(project(":compiler:fir:tree"))

    implementation(kotlinxCollectionsImmutable())
    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:psi:parser"))

    compileOnly(intellijCore())
    compileOnly(libs.guava)

    testFixturesCompileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
    "testFixtures" { projectDefault() }
}

generatedDiagnosticContainersAndCheckerComponents()
