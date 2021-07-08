import org.jetbrains.kotlin.cli.common.toBooleanLenient

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
    testCompileOnly(intellijDep()) {
        includeJars("extensions", "idea_rt", "util", "asm-all", "jna", rootProject = rootProject)
    }

    testCompileOnly(intellijPluginDep("java")) { includeJars("java-api") }

    testRuntimeOnly("xerces:xercesImpl:2.12.0")
    testRuntimeOnly(intellijDep())
    testRuntimeOnly(intellijPluginDep("java"))

    testApi(commonDep("junit:junit"))
    testCompileOnly(project(":kotlin-test:kotlin-test-jvm"))
    testCompileOnly(project(":kotlin-test:kotlin-test-junit"))
    testApi(projectTests(":compiler:tests-common"))

    testCompileOnly(project(":kotlin-reflect-api"))
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":core:descriptors.runtime"))
    testApi(projectTests(":compiler:fir:analysis-tests:legacy-fir-tests"))
    testApi(project(":compiler:fir:resolve"))
    testApi(project(":compiler:fir:dump"))

    val asyncProfilerClasspath = project.findProperty("fir.bench.async.profiler.classpath") as? String
    if (asyncProfilerClasspath != null) {
        testRuntimeOnly(files(*asyncProfilerClasspath.split(File.pathSeparatorChar).toTypedArray()))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    systemProperties(project.properties.filterKeys { it.startsWith("fir.") })
    workingDir = rootDir
    jvmArgs!!.removeIf { it.contains("-Xmx") || it.contains("-Xms") || it.contains("ReservedCodeCacheSize") }
    minHeapSize = "8g"
    maxHeapSize = "8g"
    dependsOn(":dist")

    run {
        val argsExt = project.findProperty("fir.modularized.jvm.args") as? String
        if (argsExt != null) {
            val paramRegex = "([^\"]\\S*|\".+?\")\\s*".toRegex()
            jvmArgs(paramRegex.findAll(argsExt).map { it.groupValues[1] }.toList())
        }
    }
    jvmArgs("-XX:ReservedCodeCacheSize=512m")
}

testsJar()
