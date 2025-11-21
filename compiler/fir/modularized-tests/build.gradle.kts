/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    id("project-tests-convention")
    id("java-test-fixtures")
}

repositories {
    mavenLocal()
}

val composeCompilerPlugin by configurations.creating

dependencies {
    testFixturesImplementation(intellijCore())

    testRuntimeOnly(libs.xerces)
    testRuntimeOnly(commonDependency("org.apache.commons:commons-lang3"))

    testFixturesApi(testFixtures(project(":compiler:tests-common")))

    testFixturesApi(testFixtures(project(":compiler:fir:analysis-tests:legacy-fir-tests")))
    testFixturesApi(project(":compiler:fir:resolve"))
    testFixturesApi(project(":compiler:fir:providers"))
    testFixturesApi(project(":compiler:fir:semantics"))
    testFixturesApi(project(":compiler:fir:dump"))
    testFixturesApi(platform(libs.junit.bom))
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
    "testFixtures" { projectDefault() }
}

optInToK1Deprecation()

projectTests {
    testTask(minHeapSizeMb = 8192, maxHeapSizeMb = 8192, reservedCodeCacheSizeMb = 512, jUnitMode = JUnitMode.JUnit5) {
        dependsOn(":dist", ":plugins:compose-compiler-plugin:compiler-hosted:jar")
        systemProperties(this.project.properties.filterKeys<String, Any?> { it.startsWith("fir.") })
        this.workingDir = rootDir
        systemProperty("fir.bench.compose.plugin.classpath", composeCompilerPlugin.asPath)
        val argsExt = this.project.findProperty("fir.modularized.jvm.args") as? String
        if (argsExt != null) {
            val paramRegex = "([^\"]\\S*|\".+?\")\\s*".toRegex()
            this.jvmArgs(paramRegex.findAll(argsExt).map<MatchResult, String> { it.groupValues[1] }.toList<String>())
        }
        systemProperties["junit.jupiter.execution.parallel.enabled"] = true
    }

    testGenerator(
        "org.jetbrains.kotlin.fir.generators.tests.GenerateModularizedIsolatedTests",
        generateTestsInBuildDirectory = true, skipCollectDataTask = true
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
