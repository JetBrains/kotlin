/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    Platform[193].orLower {
        testCompileOnly(intellijDep()) { includeJars("openapi", rootProject = rootProject) }
    }

    testCompileOnly(intellijDep()) {
        includeJars("extensions", "idea_rt", "util", "asm-all", rootProject = rootProject)
    }

    Platform[191].orLower {
        testCompileOnly(intellijDep()) { includeJars("java-api") }
    }

    Platform[192].orHigher {
        testCompileOnly(intellijPluginDep("java")) { includeJars("java-api") }
        testRuntimeOnly(intellijPluginDep("java"))
    }

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
    systemProperties(project.properties.filterKeys { it.startsWith("fir.") })
    workingDir = rootDir
    jvmArgs!!.removeIf { it.contains("-Xmx") }
    maxHeapSize = "8g"
    dependsOn(":dist")

    run {
        val argsExt = project.findProperty("fir.modularized.jvm.args") as? String
        if (argsExt != null) {
            val paramRegex = "([^\"]\\S*|\".+?\")\\s*".toRegex()
            jvmArgs(paramRegex.findAll(argsExt).map { it.groupValues[1] }.toList())
        }
    }
}

testsJar()