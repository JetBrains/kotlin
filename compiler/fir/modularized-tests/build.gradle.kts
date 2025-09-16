/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("project-tests-convention")
    id("java-test-fixtures")
}

repositories {
    mavenLocal()
}

val composeCompilerPlugin by configurations.creating

dependencies {
    testApi(intellijCore())

    testRuntimeOnly(libs.xerces)
    testRuntimeOnly(commonDependency("org.apache.commons:commons-lang3"))

    testApi(testFixtures(project(":compiler:tests-common")))

    testRuntimeOnly(project(":core:descriptors.runtime"))
    testApi(testFixtures(project(":compiler:fir:analysis-tests:legacy-fir-tests")))
    testFixturesApi(project(":compiler:fir:resolve"))
    testFixturesApi(project(":compiler:fir:providers"))
    testFixturesApi(project(":compiler:fir:semantics"))
    testFixturesApi(project(":compiler:fir:dump"))
    testApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testRuntimeOnly(project(":compiler:fir:plugin-utils"))

    composeCompilerPlugin(project(":plugins:compose-compiler-plugin:compiler-hosted")) { isTransitive = false }

    val asyncProfilerClasspath = project.findProperty("fir.bench.async.profiler.classpath") as? String
    if (asyncProfilerClasspath != null) {
        testRuntimeOnly(files(*asyncProfilerClasspath.split(File.pathSeparatorChar).toTypedArray()))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

optInToK1Deprecation()

projectTests {
    testTask(minHeapSizeMb = 8192, maxHeapSizeMb = 8192, reservedCodeCacheSizeMb = 512, jUnitMode = JUnitMode.JUnit5) {
        dependsOn(":dist", ":plugins:compose-compiler-plugin:compiler-hosted:jar")
        systemProperties(project.properties.filterKeys { it.startsWith("fir.") })
        workingDir = rootDir
        val composePluginClasspath = composeCompilerPlugin.asPath

        run {
            systemProperties["junit.jupiter.execution.parallel.enabled"] = true
            systemProperties["junit.jupiter.execution.parallel.mode.default"] = "concurrent"
            maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
            systemProperty("fir.bench.compose.plugin.classpath", composePluginClasspath)
            val argsExt = project.findProperty("fir.modularized.jvm.args") as? String
            if (argsExt != null) {
                val paramRegex = "([^\"]\\S*|\".+?\")\\s*".toRegex()
                jvmArgs(paramRegex.findAll(argsExt).map { it.groupValues[1] }.toList())
            }
        }
    }
}

testsJar()

val generateIntellijTestData by generator("org.jetbrains.kotlin.fir.generators.tests.GenerateModularizedTests") {
    systemProperty("fir.modularized.test.model.path",kotlinBuildProperties.pathToIntellijModularizedTestData?.let { "$it/test-project-model-dump" } ?: "")
    systemProperty("fir.modularized.test.project.name", "IntelliJ")
}
