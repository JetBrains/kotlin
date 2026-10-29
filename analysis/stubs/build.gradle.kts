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
    api(project(":compiler:psi:psi-api"))
    api(project(":core:compiler.common"))
    api(project(":core:metadata"))
    implementation(project(":compiler:psi:psi-impl"))
    implementation(project(":compiler:psi:parser"))
    implementation(project(":core:error.handling"))
    implementation(project(":core:util.runtime"))

    compileOnly(intellijCore())
    compileOnly(libs.org.jetbrains.annotations)

    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-fir")))
    testFixturesApi(testFixtures(project(":analysis:low-level-api-fir")))
    testFixturesApi(project(":analysis:decompiled:decompiler"))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    jvmAbiGenPlugin(project(":plugins:jvm-abi-gen"))
}

sourceSets {
    "main" { projectDefault() }

    "test" {
        projectDefault()
        generatedTestDir()
    }

    "testFixtures" { projectDefault() }
}

tasks.compileKotlin {
    compilerOptions {
        // The stub implementation builds the PSI from stubs, so it opts in to all of psi-api's non-public markers
        // (the same list psi-api excludes from its ABI dump) instead of repeating the suppression in every file.
        optIn.addAll(
            listOf(
                "org.jetbrains.kotlin.psi.KtImplementationDetail",
                "org.jetbrains.kotlin.psi.KtNonPublicApi",
                "org.jetbrains.kotlin.psi.KtIdeApi",
                "org.jetbrains.kotlin.psi.KtExperimentalApi",
                "org.jetbrains.kotlin.psi.KtPlatformInterface",
            )
        )
    }
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

    @OptIn(KotlinCompilerDistUsage::class)
    withDist()

    testData(project.isolated, "testData")
    testData(project(":compiler:psi:psi-impl").isolated, "testData/psi")
}

