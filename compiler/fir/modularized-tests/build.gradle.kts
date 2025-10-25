/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("project-tests-convention")
}

repositories {
    mavenLocal()
}

val composeCompilerPlugin by configurations.creating

dependencies {
    testImplementation(intellijCore())

    testRuntimeOnly(libs.xerces)
    testRuntimeOnly(commonDependency("org.apache.commons:commons-lang3"))

    testImplementation(libs.junit4)
    testCompileOnly(kotlinTest("junit"))
    testImplementation(testFixtures(project(":compiler:tests-common")))

    testRuntimeOnly(project(":core:descriptors.runtime"))
    testImplementation(testFixtures(project(":compiler:fir:analysis-tests:legacy-fir-tests")))
    testImplementation(project(":compiler:fir:resolve"))
    testImplementation(project(":compiler:fir:providers"))
    testImplementation(project(":compiler:fir:semantics"))
    testImplementation(project(":compiler:fir:dump"))

    testRuntimeOnly(project(":compiler:fir:plugin-utils"))

    composeCompilerPlugin(project(":plugins:compose-compiler-plugin:compiler-hosted")) { isTransitive = false }

    val asyncProfilerClasspath = project.findProperty("fir.bench.async.profiler.classpath") as? String
    if (asyncProfilerClasspath != null) {
        testRuntimeOnly(files(*asyncProfilerClasspath.split(File.pathSeparatorChar).toTypedArray()))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

optInToK1Deprecation()

projectTests {
    val modelDumpAndReadTest = "org.jetbrains.kotlin.fir.ModelDumpAndReadTest"

    testTask(minHeapSizeMb = 8192, maxHeapSizeMb = 8192, reservedCodeCacheSizeMb = 512, jUnitMode = JUnitMode.JUnit4) {
        dependsOn(":dist", ":plugins:compose-compiler-plugin:compiler-hosted:jar")
        systemProperties(project.properties.filterKeys { it.startsWith("fir.") })
        workingDir = rootDir
        val composePluginClasspath = composeCompilerPlugin.asPath

        filter {
            excludeTestsMatching(modelDumpAndReadTest)
        }
        run {
            systemProperty("fir.bench.compose.plugin.classpath", composePluginClasspath)
            val argsExt = project.findProperty("fir.modularized.jvm.args") as? String
            if (argsExt != null) {
                val paramRegex = "([^\"]\\S*|\".+?\")\\s*".toRegex()
                jvmArgs(paramRegex.findAll(argsExt).map { it.groupValues[1] }.toList())
            }
        }
    }

    testTask("modelDumpTest", jUnitMode = JUnitMode.JUnit4, skipInLocalBuild = false) {
        dependsOn(":dist")
        workingDir = rootDir
        filter {
            includeTestsMatching(modelDumpAndReadTest)
        }
    }
}

testsJar()
