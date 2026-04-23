
description = "Kotlin Java Direct Compiler Plugin"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("test-inputs-check")
    id("java-test-fixtures")
    id("project-tests-convention")
}

dependencies {
    api(project(":core:compiler.common.jvm"))

    compileOnly(intellijCore())
    implementation(project(":compiler:frontend.common.jvm"))
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

projectTests {
    testTask(
        javaLauncher = JdkMajorVersion.JDK_1_8,
        defineJDKEnvVariables = listOf(
            JdkMajorVersion.JDK_1_8,
            JdkMajorVersion.JDK_11_0,
            JdkMajorVersion.JDK_17_0,
            JdkMajorVersion.JDK_21_0
        )
    ) {}
    withJvmStdlibAndReflect()
    withScriptRuntime()
    withMockJdkAnnotationsJar()
    withTestJar()
    withScriptingPlugin()
    withMockJdkRuntime()
    withStdlibCommon()
    withAnnotations()
    withThirdPartyAnnotations()
    withThirdPartyJava8Annotations()
    withThirdPartyJsr305()
}
