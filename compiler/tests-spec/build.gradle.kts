plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("project-tests-convention")
    id("test-inputs-check")
    id("java-test-fixtures")
}

dependencies {
    testFixturesImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(testFixtures(project(":compiler:tests-common")))

    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))

    testFixturesImplementation(commonDependency("com.google.code.gson:gson"))
    testImplementation(commonDependency("com.google.code.gson:gson"))
    testFixturesImplementation(intellijJDom())
    testImplementation(intellijJDom())

    api(libs.jsoup)

    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(toolsJar())
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
    runtimeOnly(libs.junit.vintage.engine)
    testFixturesImplementation(libs.junit4)
    testImplementation(libs.junit4)
}

sourceSets {
    "main" { }
    "testFixtures" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()

val generateFeatureInteractionSpecTestData by generator("org.jetbrains.kotlin.spec.utils.tasks.GenerateFeatureInteractionSpecTestDataKt", testSourceSet)

val printSpecTestsStatistic by generator("org.jetbrains.kotlin.spec.utils.tasks.PrintSpecTestsStatisticKt", testSourceSet)

val specConsistencyTests by task<Test> {
    filter {
        includeTestsMatching("org.jetbrains.kotlin.spec.consistency.SpecTestsConsistencyTest")
    }
    useJUnitPlatform()
}

projectTests {
    testData(isolated, "testData")
    testData(project(":compiler").isolated, "testData")

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withTestJar()
    withMockJdkAnnotationsJar()
    withMockJdkRuntime()
    withStdlibCommon()

    testTask(jUnitMode = JUnitMode.JUnit5) {
        filter {
            excludeTestsMatching("org.jetbrains.kotlin.spec.consistency.SpecTestsConsistencyTest")
        }
    }

    testGenerator(
        "org.jetbrains.kotlin.spec.utils.tasks.GenerateSpecTestsKt",
        taskName = "generateSpecTests",
    )
}
