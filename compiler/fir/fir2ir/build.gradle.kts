plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    compileOnly(project(":core:descriptors"))
    compileOnly(project(":core:descriptors.jvm"))
    compileOnly(project(":compiler:fir:cones"))
    compileOnly(project(":compiler:fir:resolve"))
    compileOnly(project(":compiler:fir:providers"))
    compileOnly(project(":compiler:fir:semantics"))
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:ir.serialization.common"))
    compileOnly(project(":compiler:fir:fir-serialization"))
    compileOnly(project(":compiler:fir:fir-deserialization"))
    compileOnly(project(":compiler:frontend.common.jvm"))
    compileOnly(project(":compiler:config.jvm"))
    compileOnly(project(":compiler:frontend"))

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

    testRuntimeOnly(project(":core:deserialization"))
    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":core:descriptors.jvm"))
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

optInToObsoleteDescriptorBasedAPI()

sourceSets {
    "main" { projectDefault() }
    "testFixtures" { projectDefault() }
}

fun Test.configure(configureJUnit: JUnitPlatformOptions.() -> Unit = {}) {
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
    testData(project(":compiler:tests-spec").isolated, "testData/codegen")
    testTask(
        jUnitMode = JUnitMode.JUnit5,
        defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_1_8, JdkMajorVersion.JDK_11_0, JdkMajorVersion.JDK_17_0, JdkMajorVersion.JDK_21_0),
    ) {
        configure()
    }

    testTask("aggregateTests", jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = true) {
        configure {
            excludeTags("FirPsiCodegenTest")
        }

    }

    testTask("nightlyTests", jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = true) {
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
}

testsJarToBeUsedAlongWithFixtures()
