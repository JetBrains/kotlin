import org.jetbrains.kotlin.testFederation.testFederation

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("test-inputs-check")
    id("test-data-manager")
}

dependencies {
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testFixturesApi(intellijCore())
    testFixturesApi(project(":compiler:fir:resolve"))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))
    testFixturesApi(testFixtures(project(":analysis:low-level-api-fir")))
    testFixturesApi(testFixtures(project(":analysis:test-data-manager")))
    testFixturesImplementation(testFixtures(project(":js:js.tests")))
}

sourceSets {
    "main" { none() }
    "test" { generatedTestDir() }
    "testFixtures" { projectDefault() }
}

if (!kotlinBuildProperties.isTeamcityBuild.get()) {
    testDataManager {
        // The compiler's diagnostic tests are the golden tests of the LL diagnostic tests
        mustRunAfterProjects.add(":compiler:jvm.tests")
        mustRunAfterProjects.add(":plugins:plugin-sandbox")
        mustRunAfterProjects.add(":plugins:scripting:scripting-tests")
    }
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "org.jetbrains.kotlin.fir.symbols.SymbolInternals",
            "org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess",
            "org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals",
            "org.jetbrains.kotlin.analysis.api.KaImplementationDetail",
            "org.jetbrains.kotlin.analysis.api.KaExperimentalApi",
            "org.jetbrains.kotlin.analysis.api.KaContextParameterApi",
            "org.jetbrains.kotlin.analysis.api.KaPlatformInterface",
            "org.jetbrains.kotlin.analysis.api.KaSpiExtensionPoint",
        )
    }
}

testDataManager {
    testPackages.add("org.jetbrains.kotlin.analysis.low.level.api.fir")
}

projectTests {
    testTask(
        maxHeapSize = testMaxHeapSizeLarge,
        defineJDKEnvVariables = listOf(
            JdkMajorVersion.JDK_11_0, // TestsWithJava11 and others
            JdkMajorVersion.JDK_17_0, // TestsWithJava17 and others
            JdkMajorVersion.JDK_21_0, // TestsWithJava21 and others
            JdkMajorVersion.JDK_25_0, // TestsWithJava25 and others
        )
    ) {
        testFederation {
            smokeTests {
                includeAutoSamples(percentage = 1)
            }
        }
    }

    testGenerator("org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.TestGeneratorKt", generateTestsInBuildDirectory = true)

    testData(project(":compiler").isolated, "testData/diagnostics")
    testData(project(":compiler").isolated, "testData/codegen")
    testData(project(":compiler").isolated, "tests-spec/testData/diagnostics")
    testData(project(":compiler:fir:raw-fir").isolated, "testData")
    testData(project(":js:js.translator").isolated, "testData/_commonFiles")
    testData(project(":plugins:scripting:scripting-tests").isolated, "testData/diagnostics")
    testData(project(":plugins:scripting:scripting-tests").isolated, "testData/codegen")
    testData(project(":plugins:plugin-sandbox").isolated, "testData/diagnostics")
    testData(project(":plugins:plugin-sandbox").isolated, "testData/box")

    withJvmStdlibAndReflect()
    withStdlibCommon()
    withStdlibWeb()
    withJsRuntime()
    withWasmRuntime()
    withAnnotations()
    withThirdPartyJava8Annotations()
    withThirdPartyJsr305()
    withTestJar()
    withMockJdkRuntime()
    withMockJDKModifiedRuntime()
    withMockJdkAnnotationsJar()
    withScriptingPlugin()
    withTestScriptDefinition()
    withPluginSandboxAnnotations()
    withPluginSandboxJar()
}
