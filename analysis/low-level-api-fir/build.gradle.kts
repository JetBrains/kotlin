import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-data-manager")
    id("test-inputs-check")
}

dependencies {
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    api(project(":compiler:psi:psi-api"))
    api(project(":compiler:fir:fir2ir"))
    api(project(":compiler:fir:fir2ir:jvm-backend"))
    api(project(":compiler:ir.serialization.common"))
    api(project(":compiler:fir:resolve"))
    api(project(":compiler:fir:providers"))
    api(project(":compiler:fir:semantics"))
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:checkers:checkers.jvm"))
    api(project(":compiler:fir:checkers:checkers.js"))
    api(project(":compiler:fir:checkers:checkers.native"))
    api(project(":compiler:fir:checkers:checkers.wasm"))
    api(project(":compiler:fir:fir-jvm"))
    api(project(":compiler:backend.common.jvm"))
    api(project(":compiler:cli-base"))
    implementation(project(":native:native.config"))
    implementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    implementation(project(":analysis:decompiled:decompiler-to-psi"))
    testFixturesApi(project(":analysis:analysis-api-fir"))
    testFixturesImplementation(project(":native:native.config"))

    implementation(project(":compiler:frontend.common"))
    implementation(project(":compiler:fir:entrypoint"))
    implementation(project(":analysis:analysis-api-platform-interface"))
    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-internal-utils"))
    implementation(project(":analysis:analysis-api-standalone:analysis-api-standalone-base"))
    implementation(project(":kotlin-scripting-compiler"))
    implementation(project(":kotlin-scripting-common"))
    implementation(project(":kotlin-assignment-compiler-plugin.k2"))
    implementation(project(":kotlin-assignment-compiler-plugin.cli"))
    implementation(libs.caffeine)

    implementation(libs.opentelemetry.api)

    api(intellijCore())

    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))

    testFixturesApi(libs.opentest4j)
    testFixturesApi(project(":analysis:analysis-api-standalone:analysis-api-fir-standalone-base"))
    testFixturesCompileOnly(toolsJarApi())
    testRuntimeOnly(toolsJar())
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:fir:analysis-tests:legacy-fir-tests")))
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-impl-base")))
    testFixturesApi(testFixtures(project(":compiler:fir:raw-fir:psi2fir")))
    testFixturesApi(kotlinTest("junit"))
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testFixturesApi(project(":analysis:symbol-light-classes"))
    testFixturesApi(testFixtures(project(":plugins:scripting:scripting-tests")))
    testFixturesApi(project(":kotlin-scripting-common"))
    testFixturesImplementation(testFixtures(project(":analysis:decompiled:decompiler-to-psi")))
    testFixturesImplementation(testFixtures(project(":compiler:tests-spec")))

    // We use 'api' instead of 'implementation' because other modules might be using these jars indirectly
    testFixturesApi(project(":plugins:plugin-sandbox"))
    testFixturesApi(testFixtures(project(":plugins:plugin-sandbox")))
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
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
        workingDir = rootDir

        extensions.configure<TestInputsCheckExtension> {
            allowFlightRecorder = true
        }

        if (!kotlinBuildProperties.isTeamcityBuild.get()) {
            // Ensure golden tests run first since some LL tests are complementary for the surface tests
            mustRunAfter(":analysis:analysis-api-fir:test")
        }
    }

    testGenerator("org.jetbrains.kotlin.analysis.low.level.api.fir.TestGeneratorKt")

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
    withScriptingPlugin()
    withTestScriptDefinition()
    withDist()
    withPluginSandboxAnnotations()
}

allprojects {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.optIn.addAll(
            listOf(
                "org.jetbrains.kotlin.fir.symbols.SymbolInternals",
                "org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess",
                "org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals",
                "org.jetbrains.kotlin.analysis.api.KaImplementationDetail",
                "org.jetbrains.kotlin.analysis.api.KaExperimentalApi",
                "org.jetbrains.kotlin.analysis.api.KaContextParameterApi",
            )
        )
    }
}

testsJar()

tasks.register("analysisLowLevelApiFirAllTests") {
    dependsOn(
        ":analysis:low-level-api-fir:test",
        ":analysis:low-level-api-fir:low-level-api-fir-compiler-tests:test",
    )

    if (kotlinBuildProperties.isKotlinNativeEnabled.get()) {
        dependsOn(
            ":analysis:low-level-api-fir:low-level-api-fir-native-compiler-tests:llFirNativeTests",
        )
    }
}
