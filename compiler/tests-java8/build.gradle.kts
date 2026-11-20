plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("test-inputs-check")
    id("java-test-fixtures")
}

dependencies {
    testImplementation(project(":kotlin-scripting-compiler"))
    testImplementation(project(":core:compiler.common.jvm"))
    testImplementation(project(":compiler:cli"))
    testImplementation(project(":compiler:cli-base"))
    testImplementation(project(":compiler:cli-jvm"))
    testImplementation(project(":compiler:config"))
    testImplementation(project(":compiler:fir:entrypoint"))
    testImplementation(project(":compiler:fir:fir-jvm"))
    testImplementation(project(":compiler:fir:semantics.api"))
    testImplementation(project(":compiler:fir:tree"))
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(intellijCore())

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(testFixtures(project(":generators:test-generator")))
    testRuntimeOnly(toolsJar())
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

projectTests {
    testData(project(":compiler").isolated, "testData/builtin-classes")

    withJvmStdlibAndReflect()
    withScriptingPlugin()
    withTestJar()
    withAnnotations()
    withMockJdkAnnotationsJar()
    withThirdPartyJava8Annotations()

    testTask(
        defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_21_0),
        maxHeapSize = testMaxHeapSizeLarge,
    ) {
        systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
    }
}
