plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("compiler-tests-convention")
    id("test-inputs-check")
}

dependencies {
    testApi(testFixtures(project(":compiler:tests-common")))

    testImplementation(testFixtures(project(":compiler:test-infrastructure")))
    testImplementation(testFixtures(project(":compiler:tests-common-new")))

    testApi(commonDependency("com.google.code.gson:gson"))
    testApi(intellijJDom())

    api(libs.jsoup)

    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(toolsJar())
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
    runtimeOnly(libs.junit.vintage.engine)
    testImplementation(libs.junit4)
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar()

projectTest(parallel = true) {
    useJUnitPlatform()
}

val generateSpecTests by generator("org.jetbrains.kotlin.spec.utils.tasks.GenerateSpecTestsKt")

val generateFeatureInteractionSpecTestData by generator("org.jetbrains.kotlin.spec.utils.tasks.GenerateFeatureInteractionSpecTestDataKt")

val printSpecTestsStatistic by generator("org.jetbrains.kotlin.spec.utils.tasks.PrintSpecTestsStatisticKt")

val specConsistencyTests by task<Test> {
    filter {
        includeTestsMatching("org.jetbrains.kotlin.spec.consistency.SpecTestsConsistencyTest")
    }
    useJUnitPlatform()
}

compilerTests {
    testData(isolated, "testData")
    testData(project(":compiler").isolated, "testData")
    withScriptRuntime()
    withTestJar()
    withMockJdkAnnotationsJar()
    withMockJdkRuntime()
    withStdlibCommon()
}

tasks.named<Test>("test") {
    filter {
        excludeTestsMatching("org.jetbrains.kotlin.spec.consistency.SpecTestsConsistencyTest")
    }
    if (System.getenv("COMPILER_TESTS_REMOTE_DEBUG").equals("true", ignoreCase = true)) {
        jvmArgs("-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005")
    }
}
