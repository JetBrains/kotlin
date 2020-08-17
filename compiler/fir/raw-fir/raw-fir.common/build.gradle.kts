/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:psi"))
    compile(project(":core:descriptors"))
    compile(project(":compiler:fir:tree"))
    compile("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.2")

    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "guava", rootProject = rootProject) }

    Platform[192].orHigher {
        testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
        testRuntimeOnly(intellijCoreDep()) { includeJars("intellij-core") }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

projectTest(parallel = true) {
    workingDir = rootDir
}