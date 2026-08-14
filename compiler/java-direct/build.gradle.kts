
description = "Kotlin Java Direct Compiler Plugin"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("test-inputs-check")
    id("java-test-fixtures")
}

dependencies {
    api(project(":core:compiler.common.jvm"))

    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)
    implementation(project(":compiler:frontend.common.jvm"))
    implementation(project(":compiler:plugin-api"))
    implementation(project(":compiler:cli"))

    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-compiler-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(project(":compiler:cli"))
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)

    testFixturesImplementation(testFixtures(project(":generators:test-generator")))

    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

optInToExperimentalCompilerApi()

projectTests {
    testTask(
        javaLauncher = JdkMajorVersion.JDK_1_8,
        maxHeapSize = testMaxHeapSizeLarge,
        // Use Parallel GC because this test runs on JDK 8.
        garbageCollector = GarbageCollector.Parallel,
        defineJDKEnvVariables = listOf(
            JdkMajorVersion.JDK_1_8,
            JdkMajorVersion.JDK_11_0,
            JdkMajorVersion.JDK_17_0,
            JdkMajorVersion.JDK_21_0,
            JdkMajorVersion.JDK_25_0,
        )
    ) {}
    testGenerator("org.jetbrains.kotlin.java.direct.TestGeneratorKt", generateTestsInBuildDirectory = true)
    testData(project(":compiler:fir:analysis-tests").isolated, "testData")
    testData(project(":compiler").isolated, "testData/codegen")
    testData(project(":compiler").isolated, "testData/diagnostics")
    testData(project(":compiler").isolated, "testData/loadJava")

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withMockJdkAnnotationsJar()
    withMockJDKModifiedRuntime()
    withTestJar()
    withScriptingPlugin()
    withMockJdkRuntime()
    withStdlibCommon()
    withAnnotations()
    withThirdPartyJsr305()
    withThirdPartyAnnotations()
    withThirdPartyJava8Annotations()
    withThirdPartyJava9Annotations()
}
