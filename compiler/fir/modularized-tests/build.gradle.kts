/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompileOnly(intellijDep()) { includeJars("openapi", "idea", "idea_rt", "util", "asm-all", rootProject = rootProject) }
    testRuntime(intellijDep())

    testCompile(commonDep("junit:junit"))
    testCompileOnly(project(":kotlin-test:kotlin-test-jvm"))
    testCompileOnly(project(":kotlin-test:kotlin-test-junit"))
    testCompile(projectTests(":compiler:tests-common"))

    testCompileOnly(project(":kotlin-reflect-api"))
    testRuntime(project(":kotlin-reflect"))
    testCompile(projectTests(":compiler:fir:resolve"))
    testCompile(project(":compiler:fir:resolve"))
    testCompile(project(":compiler:fir:dump"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
    jvmArgs!!.removeIf { it.contains("-Xmx") }
    maxHeapSize = "3g"
}

testsJar()