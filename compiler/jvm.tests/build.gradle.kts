import org.jetbrains.kotlin.testFederation.DelicateTestFederationApi
import org.jetbrains.kotlin.testFederation.Domain
import org.jetbrains.kotlin.testFederation.testFederationDomains

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("test-inputs-check")
    id("test-coverage-convention")
}

dependencies {
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-compiler-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))
    testFixturesImplementation(testFixtures(project(":compiler:tests-spec")))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testRuntimeOnly(project(":compiler:fir:fir2ir:jvm-backend"))
    testRuntimeOnly(project(":kotlin-util-klib-abi"))
    testRuntimeOnly(project(":generators"))

    testRuntimeOnly(intellijCore())

    testRuntimeOnly(toolsJar())
    testRuntimeOnly(libs.intellij.fastutil)
}

kotlin {
    compilerOptions.optIn.addAll(
        listOf(
            "org.jetbrains.kotlin.fir.symbols.SymbolInternals",
            "org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess",
            "org.jetbrains.kotlin.types.model.K2Only",
        )
    )
}
optInToObsoleteDescriptorBasedAPI()

sourceSets {
    "main" { none() }
    "testFixtures" { projectDefault() }
    "test" { projectDefault() }
}

fun Test.configure(configureJUnit: JUnitPlatformOptions.() -> Unit = {}) {
    javaLauncher = project.getToolchainLauncherFor(JdkMajorVersion.JDK_1_8)
    useJUnitPlatform {
        configureJUnit()
    }

    @OptIn(DelicateTestFederationApi::class)
    testFederationDomains = listOf(Domain.Jvm)
}

projectTests {
    testData(project(":compiler").isolated, "testData/codegen")
    testData(project(":compiler").isolated, "testData/diagnostics")
    testData(project(":compiler").isolated, "testData/ir")
    testData(project(":compiler").isolated, "testData/klib")
    testData(project(":compiler").isolated, "testData/debug")
    testData(project(":compiler").isolated, "testData/checkLocalVariablesTable")
    testData(project(":compiler").isolated, "testData/writeSignature")
    testData(project(":compiler").isolated, "testData/writeFlags")
    testData(project(":compiler:tests-spec").isolated, "testData/codegen")

    val environment = listOf(JdkMajorVersion.JDK_1_8, JdkMajorVersion.JDK_11_0, JdkMajorVersion.JDK_17_0, JdkMajorVersion.JDK_21_0)
    testTask(
        defineJDKEnvVariables = environment,
        maxHeapSize = testMaxHeapSizeLarge,
        garbageCollector = GarbageCollector.Parallel
    ) {
        configure()
    }

    testTask(
        "aggregateTests",
        defineJDKEnvVariables = environment,
        skipInLocalBuild = true,
        maxHeapSize = testMaxHeapSizeLarge,
        garbageCollector = GarbageCollector.Parallel
    ) {
        configure {
            excludeTags("FirPsiCodegenTest")
        }
    }

    testTask(
        "nightlyTests",
        defineJDKEnvVariables = environment,
        skipInLocalBuild = true,
    ) {
        configure {
            includeTags("FirPsiCodegenTest")
        }
    }

    testGenerator("org.jetbrains.kotlin.test.TestGeneratorForJvmTestsKt", generateTestsInBuildDirectory = true)

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withMockJdkAnnotationsJar()
    withTestJar()
    withScriptingPlugin()
    withMockJdkRuntime()
    withStdlibCommon()
    withAnnotations()
    withThirdPartyAnnotations()
    withThirdPartyJsr305()
    withThirdPartyJava8Annotations()
}

testsJarToBeUsedAlongWithFixtures()
