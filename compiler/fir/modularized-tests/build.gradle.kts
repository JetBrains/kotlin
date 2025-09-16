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
    testImplementation(intellijCore())

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
    val modularizedTests = "org.jetbrains.kotlin.fir.*ModularizedTest"
    val generatedMtIsolatedTests = "org.jetbrains.kotlin.fir.*FullPipelineTestsGenerated"

    fun Test.setUpModularizedTests() {
        dependsOn(":dist", ":plugins:compose-compiler-plugin:compiler-hosted:jar")
        systemProperties(project.properties.filterKeys { it.startsWith("fir.") })
        workingDir = rootDir
        systemProperty("fir.bench.compose.plugin.classpath", composeCompilerPlugin.asPath)
        val argsExt = project.findProperty("fir.modularized.jvm.args") as? String
        if (argsExt != null) {
            val paramRegex = "([^\"]\\S*|\".+?\")\\s*".toRegex()
            jvmArgs(paramRegex.findAll(argsExt).map { it.groupValues[1] }.toList())
        }
    }

    testTask(minHeapSizeMb = 8192, maxHeapSizeMb = 8192, reservedCodeCacheSizeMb = 512, jUnitMode = JUnitMode.JUnit5) {
        setUpModularizedTests()
        filter {
            excludeTestsMatching(modularizedTests)
            excludeTestsMatching(generatedMtIsolatedTests)
        }
    }

    testTask(taskName = "parallelMtIsolatedTests", minHeapSizeMb = 8192, maxHeapSizeMb = 8192, reservedCodeCacheSizeMb = 512, jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = false) {
        setUpModularizedTests()

        filter {
            includeTestsMatching(generatedMtIsolatedTests)
        }
        systemProperties["junit.jupiter.execution.parallel.enabled"] = true
        systemProperties["junit.jupiter.execution.parallel.mode.default"] = "concurrent"
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
    }

    testTask("modularizedTests", jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = false) {
        dependsOn(":dist")
        workingDir = rootDir
        filter {
            includeTestsMatching(modularizedTests)
        }
    }

    testGenerator(
        "org.jetbrains.kotlin.fir.generators.tests.GenerateModularizedIsolatedTests",
        doNotSetFixturesSourceSetDependency = true, generateTestsInBuildDirectory = true, skipCollectDataTask = true
    ) {
        fun String?.withModelDumpOrEmpty() = this?.let { "$it/test-project-model-dump" }.orEmpty()
        args = args!! + "--" +
                "Kotlin" + kotlinBuildProperties.pathToKotlinModularizedTestData.withModelDumpOrEmpty() +
                "IntelliJ" + kotlinBuildProperties.pathToIntellijModularizedTestData.withModelDumpOrEmpty() +
                "YouTrack" + kotlinBuildProperties.pathToYoutrackModularizedTestData.withModelDumpOrEmpty() +
                "Space" + kotlinBuildProperties.pathToSpaceModularizedTestData.withModelDumpOrEmpty()
    }
}

testsJar()
