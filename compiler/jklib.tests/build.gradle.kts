plugins {
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check")
    id("java-test-fixtures")
}

dependencies {
    testFixturesApi(testFixtures(project(":generators:test-generator")))
    testFixturesApi(testFixtures(project(":compiler:tests-integration")))
    testFixturesImplementation(project(":compiler:cli-jklib"))
    testFixturesImplementation(project(":compiler:ir.serialization.jklib"))

    testFixturesApi("org.junit.jupiter:junit-jupiter")
}

sourceSets {
    "main" { }
    "testFixtures" { projectDefault() }
}

projectTests {
    testData(project(":compiler").isolated, "testData/ir/irText")
    testData(rootProject.isolated, "third-party/mockJDKs/mockJDK")

    withJvmStdlibAndReflect()
    withMockJdkRuntime()
    withMockJdkAnnotationsJar()
    withThirdPartyJava8Annotations()
    withThirdPartyJsr305()

    testTask(
        jUnitMode = JUnitMode.JUnit5,
        defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_1_8, JdkMajorVersion.JDK_11_0, JdkMajorVersion.JDK_17_0)
    ) {
        val klibProvider = objects.newInstance<SystemPropertyClasspathProvider>().apply {
            property.set("kotlin.stdlib.jklib.for.test")
            classpath.from(stdlibJvmIr.elements.map { it.filter { it.asFile.name.endsWith(".klib") } })
        }
        jvmArgumentProviders.add(klibProvider)
    }

    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateJklibTestsKt", generateTestsInBuildDirectory = true)
}

val stdlibJvmIr by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    stdlibJvmIr(project(":kotlin-stdlib-jklib-for-test", configuration = "distMinimalJKlib"))
    testRuntimeOnly(files(stdlibJvmIr))
}

tasks.named("generateTestsWriteClassPath") {
    inputs.files(stdlibJvmIr)
}
