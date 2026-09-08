/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("d8-configuration")
    id("share-foreign-java-nullability-annotations")
    id("java-test-fixtures")
    id("test-inputs-check")
    id("require-explicit-types")
}

dependencies {
    compileOnly(intellijCore())

    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-compiler-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(project(":compiler:cli"))
    testFixturesApi(project(":compiler:fir:checkers"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.jvm"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.js"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.native"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.wasm"))
    testFixturesApi(project(":compiler:fir:fir-serialization"))
    testFixturesApi(project(":compiler:fir:entrypoint"))
    testFixturesApi(project(":compiler:frontend"))
    testFixturesImplementation(project(":js:js.frontend"))
    testFixturesImplementation(project(":wasm:wasm.frontend"))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))
    testFixturesImplementation(testFixtures(project(":compiler:tests-spec")))

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testRuntimeOnly(project(":compiler:fir:fir2ir:jvm-backend"))

    testFixturesApi(intellijCore())

    testRuntimeOnly(libs.intellij.fastutil)
    testRuntimeOnly(commonDependency("one.util:streamex"))
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.jna:jna"))
    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))
    testRuntimeOnly("com.jetbrains.intellij.platform:util-xml-dom:$intellijVersion") { isTransitive = false }
    testRuntimeOnly(toolsJar())

    thirdPartyAnnotationsClasspath(commonDependency("jakarta.annotation", "jakarta.annotation-api"))
    thirdPartyAnnotationsClasspath(commonDependency("io.vertx", "vertx-codegen"))
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

kotlin {
    compilerOptions.optIn.addAll(
        listOf(
            "org.jetbrains.kotlin.fir.symbols.SymbolInternals",
            "org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess",
            "org.jetbrains.kotlin.types.model.K2Only",
        )
    )
}

// Every per-directory test task registered by `testDataShards` below has the whole test classes
// directory on its `@Classpath`-annotated runtime classpath, and that directory holds the classes
// generated for all the testdata directories at once. Adding a testdata file regenerates and
// recompiles the class of its own directory, which changes the hash of that directory, which would
// invalidate every single per-directory task.
//
// The classpath can't be narrowed instead: a classpath entry has to stay a directory for the test JVM
// to load classes from it, and `@Classpath` normalization is not configurable per task. Ignoring the
// generated classes here is safe because they are leaf entry points which nothing else on the
// classpath references, and because each task still tracks its own class through
// `Test.candidateClassFiles`, which is derived from its `include` patterns.
normalization {
    runtimeClasspath {
        ignore("org/jetbrains/kotlin/test/runners/generated/**")
    }
}

projectTests {
    val testJdkEnvVariables = listOf(
        JdkMajorVersion.JDK_1_8,
        JdkMajorVersion.JDK_11_0,
        JdkMajorVersion.JDK_17_0,
        JdkMajorVersion.JDK_21_0,
        JdkMajorVersion.JDK_25_0,
    )

    testTask(
        javaLauncher = JdkMajorVersion.JDK_1_8,
        maxHeapSize = testMaxHeapSizeLarge,
        // Use Parallel GC because this test runs on JDK 8.
        garbageCollector = GarbageCollector.Parallel,
        defineJDKEnvVariables = testJdkEnvVariables,
    ) {
        useJUnitPlatform()
    }

    // `test` above runs everything in one task, which makes it a single all-or-nothing cache entry:
    // a change in any testdata file re-runs all of the tests. The tasks below run the very same tests
    // one testdata directory at a time, so that a change re-runs the tests of that directory only.
    // `test` is kept as is for IDE runs and for ad hoc `--tests` invocations.
    testDataShards(
        generatedPackage = "org.jetbrains.kotlin.test.runners.generated",
        roots = project(":compiler").isolated.projectDirectory.let { compilerDirectory ->
            listOf(
                compilerDirectory.dir("testData/diagnostics/tests"),
                compilerDirectory.dir("testData/diagnostics/testsWithAnyBackend"),
                compilerDirectory.dir("testData/diagnostics/testsWithStdLib"),
            )
        },
        javaLauncher = JdkMajorVersion.JDK_1_8,
        maxHeapSize = testMaxHeapSizeLarge,
        // Use Parallel GC because this test runs on JDK 8.
        garbageCollector = GarbageCollector.Parallel,
        defineJDKEnvVariables = testJdkEnvVariables,
    ) {
        useJUnitPlatform()
    }

    testGenerator("org.jetbrains.kotlin.test.TestGeneratorForFirAnalysisTestsKt", generateTestsInBuildDirectory = true)

    testData(project(":compiler").isolated, "testData/diagnostics")
    testData(project(":compiler").isolated, "testData/loadJava")
    testData(project(":compiler:tests-spec").isolated, "testData/diagnostics")

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withMockJdkAnnotationsJar()
    withMockJDKModifiedRuntime()
    withTestJar()
    withScriptingPlugin()
    withMockJdkRuntime()
    withStdlibCommon()
    withStdlibWeb()
    withAnnotations()
    withThirdPartyJsr305()
    withThirdPartyAnnotations()
    withThirdPartyJava8Annotations()
    withThirdPartyJava9Annotations()
}

testsJar()
