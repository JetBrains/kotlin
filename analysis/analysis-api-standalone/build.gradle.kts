import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.testFederation.testFederation

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("test-data-manager")
    id("test-inputs-check")
}

dependencies {
    api(project(":compiler:psi:psi-api"))
    api(project(":analysis:analysis-api"))
    testFixturesImplementation(testFixtures(project(":analysis:analysis-api-impl-base")))
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))
    testFixturesApi(testFixtures(project(":analysis:low-level-api-fir")))
    testFixturesApi(testFixtures(project(":compiler:psi:psi-api")))
    testFixturesImplementation(project(":analysis:analysis-api-standalone:analysis-api-standalone-fir"))

    testFixturesApi(kotlinTest("junit5"))
    testCompileOnly(toolsJarApi())
    testRuntimeOnly(toolsJar())
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

kotlin {
    explicitApi()

    compilerOptions {
        optIn.addAll(
            "org.jetbrains.kotlin.analysis.api.KaPlatformInterface",
            "org.jetbrains.kotlin.analysis.api.KaImplementationDetail",
        )
    }

    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        referenceDumpDir = File("api-unstable")

        filters {
            exclude.annotatedWith.addAll(
                "org.jetbrains.kotlin.analysis.api.KaImplementationDetail",
            )
        }
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

if (!kotlinBuildProperties.isTeamcityBuild.get()) {
    testDataManager {
        // Ensure golden tests run first
        mustRunAfterProjects.add(":analysis:analysis-api-fir")
    }
}

projectTests {
    testTask(defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_11_0, JdkMajorVersion.JDK_21_0)) {
        testFederation {
            smokeTests {
                includeAutoSamples(percentage = 1)
            }
        }
    }

    testCodebaseTask(dumpDirs = listOf("api", "api-unstable"))

    testGenerator("org.jetbrains.kotlin.analysis.api.standalone.fir.test.TestGeneratorKt")

    withJvmStdlibAndReflect()
    withStdlibCommon()
    withJsRuntime()
    withTestJar()
    withMockJdkRuntime()
    withMockJdkAnnotationsJar()
    withPluginSandboxAnnotations()
    withPluginSandboxJar()
    withWasmRuntime()

    @OptIn(KotlinCompilerDistUsage::class)
    withDist()

    testData(project.isolated, "testData")
    testData(project(":analysis:analysis-api").isolated, "testData")
    testData(project(":analysis:low-level-api-fir").isolated, "testData/resolveToFirSymbolPsiClass")
}

