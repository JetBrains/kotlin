import org.jetbrains.kotlin.testFederation.testFederation

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("test-data-manager")
    id("test-inputs-check")
}

val jvmAbiGenPlugin = configurations.create("jvmAbiGenPlugin") {
    isTransitive = false
}

dependencies {
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":analysis:decompiled:decompiler"))
    implementation(intellijCore())

    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-impl-base")))
    testFixturesApi(testFixtures(project(":analysis:low-level-api-fir")))
    testFixturesApi(project(":analysis:decompiled:decompiler"))
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
        testFederation {
            smokeTests {
                includeAutoSamples(percentage = 5)
            }
        }

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
