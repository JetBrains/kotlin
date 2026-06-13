plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    implementation(project(":compiler:frontend.java"))
    implementation(project(":core:deserialization"))

    compileOnly(project(":core:util.runtime"))
    compileOnly(project(":core:descriptors"))
    compileOnly(project(":core:descriptors.jvm"))
    compileOnly(project(":core:reflection.common.jvm"))

    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":generators:test-generator")))
    testFixturesImplementation(project(":core:reflection.common.jvm"))
    testFixturesApi(intellijCore())

    testFixturesApi(platform(libs.junit.bom))
    testImplementation(libs.junit4)
}

sourceSets {
    "main" { projectDefault() }
    "testFixtures" { projectDefault() }
}

optInToK1Deprecation()

projectTests {
    testData(project(":compiler").isolated, "testData/loadJava")
    testData(project(":compiler").isolated, "testData/loadJava8")
    withJvmStdlibAndReflect()
    withMockJdkAnnotationsJar()
    withMockJdkRuntime()
    withScriptRuntime()
    withTestJar()
    withAnnotations()

    testTask(jUnitMode = JUnitMode.JUnit4, javaLauncher = JdkMajorVersion.JDK_1_8)
    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateRuntimeDescriptorTestsKt", generateTestsInBuildDirectory = true)
}

testsJar()
