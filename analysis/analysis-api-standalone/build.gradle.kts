import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.testFederation.SmokeTestConfig
import org.jetbrains.kotlin.testFederation.smokeTestConfig

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("test-data-manager")
    id("test-inputs-check")
}

dependencies {
    implementation(intellijCore())
    implementation(project(":core:descriptors.jvm"))
    implementation(project(":core:language.targets.jvm"))
    implementation(project(":compiler:config.jvm"))
    implementation(project(":compiler:psi:parser"))
    implementation(project(":compiler:psi:psi-api"))
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-platform-interface"))
    api(project(":analysis:analysis-api-fir"))
    api(project(":analysis:low-level-api-fir"))
    api(project(":analysis:symbol-light-classes"))
    api(project(":analysis:decompiled:light-classes-for-decompiled"))
    api(project(":analysis:analysis-api-standalone:analysis-api-standalone-fir"))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-fir")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-impl-base")))
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))
    testFixturesApi(testFixtures(project(":analysis:low-level-api-fir")))
    testImplementation(testFixtures(project(":compiler:psi:psi-api")))

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

projectTests {
    testTask(defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_11_0, JdkMajorVersion.JDK_21_0)) {
        if (!kotlinBuildProperties.isTeamcityBuild.get()) {
            // Ensure golden tests run first
            mustRunAfter(":analysis:analysis-api-fir:test")
        }

        smokeTestConfig = SmokeTestConfig.Enabled(autoSmokeTestPercentage = 1)
    }

    testCodebaseTask(dumpDirs = listOf("api", "api-unstable"))

    testGenerator("org.jetbrains.kotlin.analysis.api.standalone.fir.test.TestGeneratorKt")

    withJvmStdlibAndReflect()
    withStdlibCommon()
    withJsRuntime()
    withTestJar()
    withMockJdkRuntime()
    withMockJdkAnnotationsJar()
    withScriptRuntime()
    withPluginSandboxAnnotations()
    withPluginSandboxJar()
    withWasmRuntime()

    @OptIn(KotlinCompilerDistUsage::class)
    withDist()

    testData(project.isolated, "testData")
    testData(project(":analysis:analysis-api").isolated, "testData")
    testData(project(":analysis:low-level-api-fir").isolated, "testData/resolveToFirSymbolPsiClass")
}

testsJar()
