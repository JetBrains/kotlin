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
    testFixturesImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(testFixtures(project(":compiler:tests-common")))

    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))

    testFixturesImplementation(commonDependency("com.google.code.gson:gson"))
    testFixturesImplementation(intellijJDom())

    api(libs.jsoup)

    testRuntimeOnly(toolsJar())
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit.jupiter.params)
}

sourceSets {
    "main" { }
    "testFixtures" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()

val generateFeatureInteractionSpecTestData by generator(
    "org.jetbrains.kotlin.spec.utils.tasks.GenerateFeatureInteractionSpecTestDataKt",
    testSourceSet,
    registerInAggregateGenerateSources = false
)

val printSpecTestsStatistic by generator(
    "org.jetbrains.kotlin.spec.utils.tasks.PrintSpecTestsStatisticKt",
    testSourceSet,
    registerInAggregateGenerateSources = false
)

projectTests {
    testData(isolated, "testData")
    testData(project(":compiler").isolated, "testData")

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withTestJar()
    withMockJdkAnnotationsJar()
    withMockJdkRuntime()
    withStdlibCommon()

    testTask {
        filter {
            excludeTestsMatching("org.jetbrains.kotlin.spec.consistency.SpecTestsConsistencyTest")
        }
    }

    testTask(taskName = "specConsistencyTests", skipInLocalBuild = false) {
        filter {
            includeTestsMatching("org.jetbrains.kotlin.spec.consistency.SpecTestsConsistencyTest")
        }
    }

    testGenerator(
        "org.jetbrains.kotlin.spec.utils.tasks.GenerateSpecTestsKt",
        taskName = "generateSpecTests",
        excludeFromAggregateGeneratorTask = true,
    )
}
