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

val warmupsParam = project.findProperty("warmups")?.toString() ?: ""
val iterationsParam = project.findProperty("iterations")?.toString() ?: ""
val includePattern = project.findProperty("include")?.toString() ?: ""
val sizeParam = project.findProperty("size")?.toString() ?: ""

benchmark {
    configurations {
        named("main") {
            iterationTime = 1 // Required param
            iterationTimeUnit = "sec" // Required param

            if (warmupsParam.isNotEmpty()) {
                warmups = warmupsParam.toInt() // Use default if the param isn't specified
            }

            if (iterationsParam.isNotEmpty()) {
                iterations = iterationsParam.toInt() // Use default if the param isn't specified
            }

            include(includePattern.ifEmpty { "*" }) // Benchmark everything if the pattern isn't specified

            if (sizeParam.isNotEmpty()) {
                // Use size from annotation arguments if the param isn't specified
                // CAUTION: large size might cause long execution time
                param("size", sizeParam.toInt())
            }
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