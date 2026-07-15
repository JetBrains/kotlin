plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check-v2")
    id("require-explicit-types")
}

dependencies {
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))
    implementation(project(":compiler:fir:cones"))
    implementation(project(":compiler:fir:resolve"))
    implementation(project(":compiler:fir:providers"))
    implementation(project(":compiler:fir:semantics"))
    implementation(project(":compiler:fir:tree"))
    implementation(project(":compiler:ir.tree"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.serialization.common"))
    implementation(project(":compiler:fir:fir-serialization"))
    implementation(project(":compiler:fir:fir-deserialization"))
    implementation(project(":compiler:frontend.common.jvm"))
    implementation(project(":compiler:config.jvm"))
    implementation(project(":compiler:fir:fir-jvm"))
    implementation(project(":compiler:frontend"))
    implementation(project(":core:compiler.common.web"))

    compileOnly(intellijCore())

    testCompileOnly(kotlinTest("junit"))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-compiler-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":compiler:fir:analysis-tests")))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))
    testFixturesImplementation(testFixtures(project(":compiler:tests-spec")))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testRuntimeOnly(project(":compiler:fir:fir2ir:jvm-backend"))
    testRuntimeOnly(project(":kotlin-util-klib-abi"))
    testRuntimeOnly(project(":generators"))

    testCompileOnly(intellijCore())
    testRuntimeOnly(intellijCore())

    testRuntimeOnly(toolsJar())
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.jna:jna"))
    testRuntimeOnly(libs.intellij.fastutil)
    testRuntimeOnly(commonDependency("one.util:streamex"))

    testRuntimeOnly(jpsModel())
    testRuntimeOnly(jpsModelImpl())
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
    "main" { projectDefault() }
    "testFixtures" { projectDefault() }
}

fun Test.configure(configureJUnit: JUnitPlatformOptions.() -> Unit = {}) {
    javaLauncher = project.getToolchainLauncherFor(JdkMajorVersion.JDK_1_8)
    useJUnitPlatform {
        configureJUnit()
    }
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
    testTask(
        defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_1_8, JdkMajorVersion.JDK_11_0, JdkMajorVersion.JDK_17_0, JdkMajorVersion.JDK_21_0),
    ) {
        configure()
    }

    testTask("aggregateTests", skipInLocalBuild = true) {
        configure {
            excludeTags("FirPsiCodegenTest")
        }

    }

    testTask("nightlyTests", skipInLocalBuild = true) {
        configure {
            includeTags("FirPsiCodegenTest")
        }
    }

    testGenerator("org.jetbrains.kotlin.test.TestGeneratorForFir2IrTestsKt", generateTestsInBuildDirectory = true)

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
