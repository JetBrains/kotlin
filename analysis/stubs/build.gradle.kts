import org.jetbrains.kotlin.testFederation.SmokeTestConfig
import org.jetbrains.kotlin.testFederation.smokeTestConfig

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-data-manager")
    id("test-inputs-check")
}

val jvmAbiGenPlugin = configurations.create("jvmAbiGenPlugin") {
    isTransitive = false
}

dependencies {
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    implementation(intellijCore())

    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-impl-base")))
    testFixturesApi(testFixtures(project(":analysis:low-level-api-fir")))
    testFixturesApi(testFixtures(project(":analysis:decompiled:decompiler-to-file-stubs")))
    testFixturesApi(testFixtures(project(":analysis:decompiled:decompiler-to-psi")))
    testFixturesImplementation(project(":analysis:analysis-internal-utils"))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    jvmAbiGenPlugin(project(":plugins:jvm-abi-gen"))
}

sourceSets {
    "test" {
        projectDefault()
        generatedTestDir()
    }

    "testFixtures" { projectDefault() }
}

tasks.compileTestFixturesKotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.analysis.api.KaImplementationDetail")
    }
}

tasks.compileTestKotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.analysis.api.KaImplementationDetail")
    }
}

projectTests {
    testTask(defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_11_0)) {
        smokeTestConfig = SmokeTestConfig.Enabled(autoSmokeTestPercentage = 5)

        addClasspathProperty(jvmAbiGenPlugin, "kotlin.jvm.abi.jar.path")
    }

    testGenerator("org.jetbrains.kotlin.analysis.stubs.TestGeneratorKt")

    withJvmStdlibAndReflect()
    withJsRuntime()
    withStdlibCommon()
    withTestJar()
    withAnnotations()
    withMockJdkRuntime()
    withMockJdkAnnotationsJar()
    withScriptRuntime()

    @OptIn(KotlinCompilerDistUsage::class)
    withDist()

    testData(project.isolated, "testData")
    testData(project(":compiler:psi:psi-impl").isolated, "testData/psi")
}

testsJar()
