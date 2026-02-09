import org.gradle.kotlin.dsl.testImplementation
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-data-manager")
    id("test-inputs-check")
}

dependencies {
    implementation(intellijCore())
    implementation(project(":compiler:psi:psi-api"))
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-platform-interface"))
    api(project(":analysis:analysis-api-fir"))
    api(project(":analysis:low-level-api-fir"))
    api(project(":analysis:symbol-light-classes"))
    api(project(":analysis:decompiled:light-classes-for-decompiled"))
    api(project(":analysis:analysis-api-standalone:analysis-api-standalone-base"))
    implementation(project(":analysis:analysis-api-standalone:analysis-api-fir-standalone-base"))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-fir")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-impl-base")))
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))
    testFixturesApi(testFixtures(project(":analysis:low-level-api-fir")))
    testImplementation(testFixtures(project(":compiler:psi:psi-api")))

    testFixturesApi(kotlinTest("junit"))
    testCompileOnly(toolsJarApi())
    testRuntimeOnly(toolsJar())
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

kotlin {
    explicitApi()

    compilerOptions {
        optIn.add("org.jetbrains.kotlin.analysis.api.KaPlatformInterface")
    }

    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        enabled.set(true)
        legacyDump.referenceDumpDir = File("api-unstable")

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

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5, defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_11_0)) {
        extensions.configure<TestInputsCheckExtension> {
            allowFlightRecorder = true
        }

        if (!kotlinBuildProperties.isTeamcityBuild.get()) {
            // Ensure golden tests run first
            mustRunAfter(":analysis:analysis-api-fir:test")
        }
    }

    testGenerator("org.jetbrains.kotlin.analysis.api.standalone.fir.test.TestGeneratorKt")

    withJvmStdlibAndReflect()
    withStdlibCommon()
    withJsRuntime()
    withTestJar()
    withMockJdkRuntime()
    withMockJdkAnnotationsJar()
    withScriptRuntime()
    withPluginSandboxAnnotations()

    @OptIn(KotlinCompilerDistUsage::class)
    withDist()

    testData(project.isolated, "src")
    testData(project.isolated, "api")
    testData(project.isolated, "api-unstable")
    testData(project.isolated, "testData")
    testData(project(":analysis:analysis-api").isolated, "testData")
    testData(project(":analysis:low-level-api-fir").isolated, "testData/resolveToFirSymbolPsiClass")
}

testsJar()
