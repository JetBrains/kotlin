/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

repositories {
    mavenLocal()
}

dependencies {
    testApi(intellijCore())

    testRuntimeOnly(libs.xerces)
    testRuntimeOnly(commonDependency("commons-lang:commons-lang"))

    testImplementation(libs.junit4)
    testCompileOnly(kotlinTest("junit"))
    testApi(projectTests(":compiler:tests-common"))

    testRuntimeOnly(project(":core:descriptors.runtime"))
    testApi(projectTests(":compiler:fir:analysis-tests:legacy-fir-tests"))
    testApi(project(":compiler:fir:resolve"))
    testApi(project(":compiler:fir:providers"))
    testApi(project(":compiler:fir:semantics"))
    testApi(project(":compiler:fir:dump"))

    testRuntimeOnly(project(":compiler:fir:plugin-utils"))

    val asyncProfilerClasspath = project.findProperty("fir.bench.async.profiler.classpath") as? String
    if (asyncProfilerClasspath != null) {
        testRuntimeOnly(files(*asyncProfilerClasspath.split(File.pathSeparatorChar).toTypedArray()))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest(minHeapSizeMb = 8192, maxHeapSizeMb = 8192, reservedCodeCacheSizeMb = 512) {
    dependsOn(":dist")
    systemProperties(project.properties.filterKeys { it.startsWith("fir.") })
    workingDir = rootDir

    run {
        val argsExt = project.findProperty("fir.modularized.jvm.args") as? String
        if (argsExt != null) {
            val paramRegex = "([^\"]\\S*|\".+?\")\\s*".toRegex()
            jvmArgs(paramRegex.findAll(argsExt).map { it.groupValues[1] }.toList())
        }
    }
}

testsJar()
