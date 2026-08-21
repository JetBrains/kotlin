/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("power-assert-convention")
    id("project-tests-convention")
    id("java-test-fixtures")
}

val composeCompilerPlugin = configurations.create("composeCompilerPlugin")

dependencies {
    testFixturesImplementation(intellijCore())

    testRuntimeOnly(libs.xerces)
    testRuntimeOnly(commonDependency("org.apache.commons:commons-lang3"))

    testFixturesApi(testFixtures(project(":compiler:tests-common")))

    testFixturesApi(testFixtures(project(":compiler:fir:analysis-tests:legacy-fir-tests")))
    testFixturesApi(project(":compiler:fir:resolve"))
    testFixturesApi(project(":compiler:fir:providers"))
    testFixturesApi(project(":compiler:fir:semantics"))
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    testRuntimeOnly(project(":compiler:fir:plugin-utils"))

    composeCompilerPlugin(project(":plugins:compose-compiler-plugin:compiler-hosted")) { isTransitive = false }

    // Used by modularized-tests
    val asyncProfilerClasspath = project.providers.gradleProperty("fir.bench.async.profiler.classpath")
    if (asyncProfilerClasspath.isPresent) {
        testRuntimeOnly(files(*asyncProfilerClasspath.get().split(File.pathSeparatorChar).toTypedArray()))
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
    testTask(
        minHeapSize = testMaxHeapSizeHuge,
        maxHeapSize = testMaxHeapSizeHuge,
        reservedCodeCacheSize = 512.MiB,
        javaLauncher = JdkMajorVersion.JDK_1_8,
        garbageCollector = null // explicitly not selecting any GC as the 'fir.modularized.jvm.args' is expected to provide such settings
    ) {
        dependsOn(":dist", ":plugins:compose-compiler-plugin:compiler-hosted:jar")
        systemProperties(providers.gradlePropertiesPrefixedBy("fir.").get())
        this.workingDir = rootDir
        systemProperty("fir.bench.compose.plugin.classpath", composeCompilerPlugin.asPath)
        // Used by modularized-tests kotlin-compiler-modularized-tests-teamcity/files/mt2025/.teamcity/projects/infrastructure/ModularizedTestStep.kt
        val modularizedJvmArgs = project.providers.gradleProperty("fir.modularized.jvm.args")
        if (modularizedJvmArgs.isPresent) {
            val paramRegex = "([^\"]\\S*|\".+?\")\\s*".toRegex()
            this.jvmArgs(paramRegex.findAll(modularizedJvmArgs.get()).map<MatchResult, String> { it.groupValues[1] }.toList<String>())
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
