/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:fir:tree"))

    implementation(kotlinxCollectionsImmutable())
    implementation(project(":compiler:psi"))

    compileOnly(intellijCore())
    compileOnly(libs.guava)

    testCompileOnly(intellijCore())
    testRuntimeOnly(intellijCore())
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDir("gen")
    }
    "test" { none() }
}

projectTest(parallel = true) {
    workingDir = rootDir
}
