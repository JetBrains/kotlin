plugins {
    kotlin("jvm")
    id("test-inputs-check")
    id("java-test-fixtures")
    id("project-tests-convention")
}

repositories {
    mavenCentral()
    maven { setUrl("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") }
}

dependencies {
    api(project(":compiler:frontend.java"))
    api(project(":core:compiler.common.jvm"))

    compileOnly(intellijCore())
    implementation(libs.org.jetbrains.syntax.api)
    implementation(libs.org.jetbrains.java.syntax.jvm)
    implementation(project(":compiler:plugin-api"))
    implementation(project(":compiler:cli"))

    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-compiler-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(project(":compiler:cli"))
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)

    testFixturesImplementation(testFixtures(project(":generators:test-generator")))

    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

optInToExperimentalCompilerApi()

kotlin {
    jvmToolchain(17)
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5, defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_17_0)) {
        useJUnitPlatform()
    }
    testGenerator("org.jetbrains.kotlin.java.direct.TestGeneratorKt", generateTestsInBuildDirectory = true)
    testData(project(":compiler:fir:analysis-tests").isolated, "testData")
    testData(project(":compiler").isolated, "testData/codegen")
    testData(project(":compiler").isolated, "testData/diagnostics")
    testData(project(":compiler").isolated, "testData/loadJava")

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
