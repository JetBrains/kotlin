plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check-v2")
    id("java-test-fixtures")
}

dependencies {
    testImplementation(project(":kotlin-scripting-compiler"))
    testImplementation(project(":core:descriptors"))
    testImplementation(project(":core:descriptors.jvm"))
    testImplementation(project(":core:compiler.common.jvm"))
    testImplementation(project(":compiler:cli-jvm:javac-integration"))
    testImplementation(testFixtures(project(":compiler:tests-compiler-utils")))
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
    withScriptRuntime()
    withScriptingPlugin()
    withTestJar()
    withAnnotations()
    withMockJdkAnnotationsJar()
    withThirdPartyJava8Annotations()

    testTask(
        defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_21_0),
        javaLauncher = JdkMajorVersion.JDK_1_8
    ) {
        systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
    }
}


optInToK1Deprecation()

testsJar()
