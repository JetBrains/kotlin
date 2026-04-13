import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.testFederation.TemporaryTestFederationApi
import org.jetbrains.kotlin.testFederation.isSmokeTest

plugins {
    kotlin("jvm")
    id("generated-sources")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-data-manager")
    id("test-inputs-check")
}

dependencies {
    implementation(project(":compiler:ir.tree"))
    api(project(":compiler:fir:entrypoint"))
    api(project(":analysis:low-level-api-fir"))
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-impl-base"))
    api(project(":analysis:light-classes-base"))
    implementation(project(":compiler:backend.jvm.entrypoint"))
    api(intellijCore())
    implementation(project(":analysis:analysis-api-platform-interface"))
    implementation(project(":analysis:analysis-internal-utils"))
    implementation(project(":analysis:kt-references"))
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

optInToUnsafeDuringIrConstructionAPI()

projectTests {
    testTask(
        jUnitMode = JUnitMode.JUnit5,
        defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_11_0)
    ) {
        useJUnitPlatform()

        @OptIn(TemporaryTestFederationApi::class)
        isSmokeTest = true

        extensions.configure<TestInputsCheckExtension> {
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
}

testsJar()

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
