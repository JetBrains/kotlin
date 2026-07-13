import org.gradle.kotlin.dsl.implementation
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-data-manager")
    id("test-inputs-check-v2")
}

dependencies {
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    api(project(":compiler:psi:psi-api"))
    api(project(":compiler:fir:fir2ir"))
    api(project(":compiler:fir:fir2ir:jvm-backend"))
    implementation(project(":compiler:ir.serialization.common"))
    api(project(":compiler:fir:resolve"))
    api(project(":compiler:fir:providers"))
    api(project(":compiler:fir:semantics"))
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:checkers:checkers.jvm"))
    api(project(":compiler:fir:checkers:checkers.js"))
    api(project(":compiler:fir:checkers:checkers.native"))
    implementation(project(":compiler:fir:checkers:checkers.wasm"))
    api(project(":compiler:fir:fir-jvm"))
    implementation(project(":core:compiler.common.native"))
    implementation(project(":compiler:backend.common.jvm"))
    implementation(project(":compiler:backend"))
    implementation(project(":compiler:cli-base"))
    implementation(project(":compiler:frontend.common.jvm"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":compiler:psi:psi-impl"))
    implementation(project(":js:js.config"))
    implementation(project(":js:js.frontend.common"))
    implementation(project(":kotlin-util-klib-metadata"))
    implementation(project(":native:frontend.native"))
    implementation(project(":native:native.config"))
    implementation(project(":wasm:wasm.config"))
    implementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    implementation(project(":analysis:decompiled:decompiler-to-psi"))
    testFixturesApi(project(":analysis:analysis-api-fir"))
    testFixturesImplementation(project(":native:native.config"))

    implementation(project(":compiler:frontend.common"))
    implementation(project(":compiler:fir:entrypoint"))
    implementation(project(":js:js.frontend"))
    implementation(project(":analysis:analysis-api-platform-interface"))
    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-impl-base"))
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

    // We use 'api' instead of 'implementation' because other modules might be using these jars indirectly
    testFixturesApi(project(":plugins:plugin-sandbox"))
    testFixturesApi(testFixtures(project(":plugins:plugin-sandbox")))

    testImplementation(testFixtures(project(":compiler:psi:psi-api")))
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
        javaLauncher = JdkMajorVersion.JDK_1_8,
        defineJDKEnvVariables = listOf(
            JdkMajorVersion.JDK_11_0, // TestsWithJava11 and others
            JdkMajorVersion.JDK_17_0, // TestsWithJava17 and others
            JdkMajorVersion.JDK_21_0  // TestsWithJava21 and others
        )
    ) {
        if (!kotlinBuildProperties.isTeamcityBuild.get()) {
            // Ensure golden tests run first since some LL tests are complementary for the surface tests
            mustRunAfter(":analysis:analysis-api-fir:test")
        }
    }

    testGenerator("org.jetbrains.kotlin.analysis.low.level.api.fir.TestGeneratorKt")

    testData(project.isolated, "testData")

    val analysisApiProject = project(":analysis:analysis-api").isolated
    testData(analysisApiProject, "testData/components/compilerFacility/compilation/codeFragments/capturing")
    testData(analysisApiProject, "testData/components/resolver")
    testData(analysisApiProject, "testData/sessions/sessionInvalidation")

    withJvmStdlibAndReflect()
    withJvmStdlibSources()
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
    withPluginSandboxAnnotations()

    @OptIn(KotlinCompilerDistUsage::class)
    withDist()

    testCodebaseTask(dumpDirs = emptyList()) {
        // Forward the source-code-update flag (used by the `analysis-api-mark-internal-apis` skill) from a Gradle property to the test
        // JVM. Combine with `-Pkotlin.test.instrumentation.disable.inputs.check=true` so the test can write to source files.
        val updateSourceCode = "kotlin.analysis.codebaseTest.internalApi.updateSourceCode"
        systemProperty(updateSourceCode, project.providers.gradleProperty(updateSourceCode).orElse("false").get())
    }
}

kotlin {
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

testsJar()

tasks.register("analysisLowLevelApiFirAllTests") {
    dependsOn(
        ":analysis:low-level-api-fir:check",
        ":analysis:low-level-api-fir:low-level-api-fir-compiler-tests:check",
    )

    if (kotlinBuildProperties.isKotlinNativeEnabled.get()) {
        dependsOn(
            ":analysis:low-level-api-fir:low-level-api-fir-native-compiler-tests:check",
        )
    }
}
