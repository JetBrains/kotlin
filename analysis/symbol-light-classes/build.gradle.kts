import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-data-manager")
    id("test-inputs-check")
}

dependencies {
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":core:compiler.common"))
    implementation(project(":analysis:light-classes-base"))
    implementation(project(":compiler:backend.common.jvm"))
    implementation(project(":analysis:analysis-api-platform-interface"))
    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-internal-utils"))
    implementation(project(":analysis:decompiled:light-classes-for-decompiled"))
    implementation(intellijCore())
    implementation(kotlinxCollectionsImmutable())

    testFixturesImplementation(project(":analysis:decompiled:light-classes-for-decompiled"))
    testFixturesApi(project(":analysis:decompiled:decompiler-to-file-stubs"))
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))
    testFixturesApi(testFixtures(project(":analysis:decompiled:decompiler-to-file-stubs")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-impl-base")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-fir")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":analysis:low-level-api-fir")))
    testFixturesApi(libs.junit.jupiter.api)
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

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5, defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_11_0, JdkMajorVersion.JDK_17_0)) {
        workingDir = rootDir

        extensions.configure<TestInputsCheckExtension> {
            allowFlightRecorder = true
        }
    }

    testGenerator("org.jetbrains.kotlin.light.classes.symbol.TestGeneratorKt")

    withJvmStdlibAndReflect()
    withStdlibCommon()
    withJsRuntime()
    withTestJar()
    withMockJdkAnnotationsJar()
    withMockJdkRuntime()
    withScriptRuntime()
    withDist()
    withPluginSandboxAnnotations()

    testData(project.isolated, "testData")
    testData(project(":compiler").isolated, "testData/asJava/lightClasses")
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.optIn.addAll(
        "org.jetbrains.kotlin.analysis.api.permissions.KaAllowProhibitedAnalyzeFromWriteAction",
        "org.jetbrains.kotlin.analysis.api.KaExperimentalApi",
        "org.jetbrains.kotlin.analysis.api.KaPlatformInterface",
        "org.jetbrains.kotlin.analysis.api.KaSpiExtensionPoint",
    )
}

testsJar()
