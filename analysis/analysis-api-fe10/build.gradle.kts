import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-data-manager")
    id("test-inputs-check")
}

dependencies {
    implementation(project(":core:descriptors.jvm"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":analysis:analysis-api-impl-base"))
    implementation(project(":analysis:analysis-internal-utils"))
    implementation(project(":analysis:kt-references"))
    implementation(project(":compiler:light-classes"))

    implementation(project(":compiler:backend"))
    implementation(project(":compiler:backend.common.jvm"))
    implementation(project(":compiler:backend.jvm"))
    implementation(project(":compiler:backend.jvm.entrypoint"))

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testFixturesImplementation(project(":analysis:analysis-api-platform-interface"))
    testFixturesImplementation(project(":analysis:analysis-api-standalone:analysis-api-standalone-base"))
    testFixturesImplementation(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesImplementation(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-impl-base")))
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))

}

sourceSets {
    "main" { projectDefault() }
    "test" { generatedTestDir() }
    "testFixtures" { projectDefault() }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        optIn.addAll(
            listOf(
                "kotlin.RequiresOptIn",
                "org.jetbrains.kotlin.analysis.api.KaImplementationDetail",
                "org.jetbrains.kotlin.analysis.api.KaExperimentalApi",
                "org.jetbrains.kotlin.analysis.api.KaNonPublicApi",
                "org.jetbrains.kotlin.analysis.api.KaIdeApi",
                "org.jetbrains.kotlin.analysis.api.KaPlatformInterface",
                "org.jetbrains.kotlin.analysis.api.permissions.KaAllowProhibitedAnalyzeFromWriteAction",
                "org.jetbrains.kotlin.analysis.api.KaContextParameterApi",
                "org.jetbrains.kotlin.analysis.api.components.KaSessionComponentImplementationDetail",
                "org.jetbrains.kotlin.analysis.api.KaSpiExtensionPoint",
            )
        )
    }
}

optInToK1Deprecation()

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5, defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_11_0)) {
        workingDir = rootDir

        extensions.configure<TestInputsCheckExtension> {
            allowFlightRecorder = true
        }

        if (!kotlinBuildProperties.isTeamcityBuild.get()) {
            // Ensure golden tests run first
            mustRunAfter(":analysis:analysis-api-fir:test")
        }
    }

    testGenerator("org.jetbrains.kotlin.analysis.api.fe10.test.TestGeneratorKt")

    testData(project(":analysis:analysis-api").isolated, "testData")

    withJvmStdlibAndReflect()
    withStdlibCommon()
    withJsRuntime()
    withWasmRuntime()
    withTestJar()
    withAnnotations()
    withMockJdkRuntime()
    withMockJdkAnnotationsJar()
    withScriptRuntime()

    @OptIn(KotlinCompilerDistUsage::class)
    withDist()
}

testsJar()
