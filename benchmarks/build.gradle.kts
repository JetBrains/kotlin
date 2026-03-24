import kotlinx.benchmark.gradle.benchmark

plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinx.benchmark)
    id("project-tests-convention")
    id("java-test-fixtures")
}

dependencies {
    testApi(kotlinStdlib())
    testApi(testFixtures(project(":compiler:tests-common")))
    testApi(project(":compiler:cli"))
    testApi(intellijCore())
    testApi(libs.kotlinx.benchmark.runtime)

    testFixturesApi(libs.junit4)
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testFixturesApi(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)

    testFixturesApi(testFixtures(project(":compiler:tests-integration")))
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

optInToK1Deprecation()

benchmark {
    configurations {
        named("main") {
            warmups = 10
            iterations = 10
            iterationTime = 1
            iterationTimeUnit = "sec"
            param("size", 1000)

            include("CommonCallsBenchmark")
            include("ControlFlowAnalysisBenchmark")

            /*include("InferenceBaselineCallsBenchmark")
            include("InferenceExplicitArgumentsCallsBenchmark")
            include("InferenceForInApplicableCandidate")
            include("InferenceFromArgumentCallsBenchmark")
            include("InferenceFromReturnTypeCallsBenchmark")*/
        }
    }
    targets {
        register("test")
    }
}

tasks.withType<JavaExec>().matching { it.name == "testBenchmark" }.configureEach {
    dependsOn(":createIdeaHomeForTests")
    systemProperty("idea.home.path", ideaHomePathForTests().get().asFile.canonicalPath)
    systemProperty("idea.use.native.fs.for.win", false)
}

projectTests {
    testTask(
        parallel = false, // Disable parallelization to get more robust performance measurements
        jUnitMode = JUnitMode.JUnit4
    ) {
        workingDir = rootDir
        useJUnitPlatform()
    }

    withJvmStdlibAndReflect()
}