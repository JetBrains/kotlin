import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.testFederation.SmokeTestConfig
import org.jetbrains.kotlin.testFederation.TemporaryTestFederationApi
import org.jetbrains.kotlin.testFederation.smokeTestConfig

plugins {
    kotlin("jvm")
    id("generated-sources")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-data-manager")
    id("test-inputs-check")
}

dependencies {
    implementation(project(":core:descriptors"))
    implementation(project(":core:language.targets.jvm"))
    implementation(project(":compiler:backend.common.jvm"))
    implementation(project(":compiler:ir.tree"))
    implementation(project(":compiler:cli-base"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":compiler:ir.psi2ir"))
    api(project(":compiler:fir:entrypoint"))
    api(project(":analysis:low-level-api-fir"))
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-impl-base"))
    api(project(":analysis:light-classes-base"))
    implementation(project(":compiler:backend.jvm.entrypoint"))
    api(intellijCore())
    implementation(project(":analysis:analysis-api-platform-interface"))
    implementation(project(":analysis:symbol-light-classes"))
    implementation(project(":native:native.config"))
    implementation(libs.caffeine)
    implementation(libs.opentelemetry.api)

    testFixturesImplementation(testFixtures(project(":analysis:low-level-api-fir")))
    testFixturesApi(project(":analysis:analysis-api-standalone:analysis-api-standalone-base"))
    testFixturesImplementation(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesImplementation(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-impl-base")))

    testFixturesImplementation(project(":analysis:analysis-api-standalone:analysis-api-fir-standalone-base"))
    testFixturesImplementation(kotlinTest("junit"))
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))

    testImplementation(testFixtures(project(":analysis:low-level-api-fir")))
    testImplementation(testFixtures(project(":compiler:psi:psi-api")))

    testCompileOnly(toolsJarApi())
    testRuntimeOnly(toolsJar())
    testFixturesApi(platform(libs.junit.bom))
    testFixturesImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
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

optInToUnsafeDuringIrConstructionAPI()

projectTests {
    testTask(
        jUnitMode = JUnitMode.JUnit5,
        javaLauncher = JdkMajorVersion.JDK_1_8,
        defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_11_0)
    ) {
        useJUnitPlatform()

        @OptIn(TemporaryTestFederationApi::class)
        smokeTestConfig = SmokeTestConfig.Enabled(autoSmokeTestPercentage = 5)

        testInputsCheck {
            allowFlightRecorder = true
        }
    }

    testGenerator("org.jetbrains.kotlin.analysis.api.fir.test.TestGeneratorKt")

    testData(project.isolated, "testData")
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
    withPluginSandboxAnnotations()
    withWasmRuntime()

    @OptIn(KotlinCompilerDistUsage::class)
    withDist()

    testCodebaseTask(dumpDirs = emptyList()) {
        // Forward the source-code-update flag (used by the `analysis-api-mark-internal-apis` skill) from a Gradle property to the test
        // JVM. Combine with `-Pkotlin.test.instrumentation.disable.inputs.check=true` so the test can write to source files.
        val updateSourceCode = "kotlin.analysis.codebaseTest.internalApi.updateSourceCode"
        systemProperty(updateSourceCode, project.providers.gradleProperty(updateSourceCode).orElse("false").get())
    }
}

allprojects {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.optIn.addAll(
            listOf(
                "org.jetbrains.kotlin.fir.symbols.SymbolInternals",
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

generatedSourcesTask(
    taskName = "generateDiagnostics",
    generatorProject = ":analysis:analysis-api-fir:analysis-api-fir-generator",
    generatorMainClass = "org.jetbrains.kotlin.analysis.api.fir.generator.MainKt",
    argsProvider = { generationRoot ->
        listOf(
            "org.jetbrains.kotlin.analysis.api.fir.diagnostics",
            generationRoot.toString(),
        )
    }
)
