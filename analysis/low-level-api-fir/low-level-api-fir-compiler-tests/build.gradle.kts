import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testFixturesApi(intellijCore())
    testFixturesApi(project(":compiler:fir:resolve"))
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))
    testFixturesApi(testFixtures(project(":analysis:low-level-api-fir")))
    testFixturesImplementation(testFixtures(project(":js:js.tests")))
    testFixturesImplementation(testFixtures(project(":compiler:tests-spec")))
}

sourceSets {
    "main" { none() }
    "test" { generatedTestDir() }
    "testFixtures" { projectDefault() }
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "org.jetbrains.kotlin.analysis.api.KaExperimentalApi",
            "org.jetbrains.kotlin.analysis.api.KaPlatformInterface",
            "org.jetbrains.kotlin.analysis.api.KaSpiExtensionPoint",
        )
    }
}

projectTests {
    testTask(
        jUnitMode = JUnitMode.JUnit5,
        defineJDKEnvVariables = listOf(
            JdkMajorVersion.JDK_11_0, // TestsWithJava11 and others
            JdkMajorVersion.JDK_17_0, // TestsWithJava17 and others
            JdkMajorVersion.JDK_21_0  // TestsWithJava21 and others
        )
    ) {
        extensions.configure<TestInputsCheckExtension> {
            allowFlightRecorder = true
        }
    }

    testGenerator("org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.TestGeneratorKt", generateTestsInBuildDirectory = true)

    testData(project(":compiler").isolated, "testData/diagnostics")
    testData(project(":compiler").isolated, "testData/codegen")
    testData(project(":compiler:tests-spec").isolated, "testData/diagnostics")
    testData(project(":compiler:fir:raw-fir:psi2fir").isolated, "testData/rawBuilder")
    testData(project(":compiler:fir:raw-fir:psi2fir").isolated, "testData/rawBuilder")
    testData(project(":compiler:fir:analysis-tests").isolated, "testData/resolve")
    testData(project(":compiler:fir:analysis-tests").isolated, "testData/resolveWithStdlib")
    testData(project(":js:js.translator").isolated, "testData/_commonFiles")
    testData(project(":plugins:scripting:scripting-tests").isolated, "testData/diagnostics")
    testData(project(":plugins:scripting:scripting-tests").isolated, "testData/codegen")
    testData(project(":plugins:plugin-sandbox").isolated, "testData/diagnostics")
    testData(project(":plugins:plugin-sandbox").isolated, "testData/box")

    withJvmStdlibAndReflect()
    withStdlibCommon()
    withJsRuntime()
    withWasmRuntime()
    withAnnotations()
    withThirdPartyJava8Annotations()
    withThirdPartyJsr305()
    withTestJar()
    withMockJdkRuntime()
    withMockJDKModifiedRuntime()
    withMockJdkAnnotationsJar()
    withScriptRuntime()
    withScriptingPlugin()
    withTestScriptDefinition()
    withPluginSandboxAnnotations()
}