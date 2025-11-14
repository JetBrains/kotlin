import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
}

val scriptingTestDefinition by configurations.creating

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
    api(project(":compiler:cli-common"))
    implementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    implementation(project(":analysis:decompiled:decompiler-to-psi"))
    testFixturesApi(project(":analysis:analysis-api-fir"))

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

    testRuntimeOnly(project(":core:descriptors.runtime"))

    // We use 'api' instead of 'implementation' because other modules might be using these jars indirectly
    testFixturesApi(project(":plugins:plugin-sandbox"))
    testFixturesApi(testFixtures(project(":plugins:plugin-sandbox")))

    scriptingTestDefinition(testFixtures(project(":plugins:scripting:test-script-definition")))
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
        dependsOn(":dist", ":plugins:scripting:test-script-definition:testJar")
        workingDir = rootDir

        val scriptingTestDefinitionClasspath = scriptingTestDefinition.asPath
        doFirst {
            systemProperty("kotlin.script.test.script.definition.classpath", scriptingTestDefinitionClasspath)
        }
    }

    withJvmStdlibAndReflect()
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
    )

    if (kotlinBuildProperties.isKotlinNativeEnabled) {
        dependsOn(
            ":analysis:low-level-api-fir:low-level-api-fir-native:llFirNativeTests",
        )
    }
}
