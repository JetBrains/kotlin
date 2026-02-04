plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-data-manager")
    id("test-inputs-check")
}

dependencies {
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    implementation(intellijCore())

    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-impl-base")))
    testFixturesApi(testFixtures(project(":analysis:low-level-api-fir")))
    testFixturesApi(testFixtures(project(":analysis:decompiled:decompiler-to-file-stubs")))
    testFixturesApi(testFixtures(project(":analysis:decompiled:decompiler-to-psi")))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "test" {
        projectDefault()
        generatedTestDir()
    }

    "testFixtures" { projectDefault() }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5, defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_11_0)) {
        workingDir = rootDir
    }

    testGenerator("org.jetbrains.kotlin.analysis.stubs.TestGeneratorKt")

    withJvmStdlibAndReflect()
    withJsRuntime()
    withStdlibCommon()
    withMockJdkRuntime()
    withMockJdkAnnotationsJar()
    withScriptRuntime()

    @OptIn(KotlinCompilerDistUsage::class)
    withDist()

    testData(project.isolated, "testData")
    testData(project(":compiler").isolated, "testData/psi")
}

testsJar()
