import kotlinx.benchmark.gradle.benchmark

plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinx.benchmark)
}

dependencies {
    api(kotlinStdlib())
    api(testFixtures(project(":compiler:tests-common")))
    api(project(":compiler:cli"))
    api(intellijCore())
    api(libs.kotlinx.benchmark.runtime)
}

sourceSets {
    "main" { projectDefault() }
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
        register("main")
    }
}

tasks.withType<Zip>().matching { it.name == "mainBenchmarkJar" }.configureEach {
    isZip64 = true
    archiveFileName.set("benchmarks.jar")
}

val benchmarkTasks = listOf("mainBenchmark")
tasks.withType<JavaExec>().matching { it.name in benchmarkTasks }.configureEach {
    dependsOn(":createIdeaHomeForTests")
    systemProperty("idea.home.path", ideaHomePathForTests().get().asFile.canonicalPath)
    systemProperty("idea.use.native.fs.for.win", false)
}

tasks.register<JavaExec>("runBenchmark") {
    dependsOn(":createIdeaHomeForTests")

    // jmhArgs example: -PjmhArgs='CommonCalls -p size=500 -f 1'
    val jmhArgs = project.providers.gradleProperty("jvmArgs")
    val resultFilePath = project.layout.buildDirectory.file("benchmarks/jmh-result.json")
    val ideaHome = ideaHomePathForTests()

    val benchmarkJarPath = project.layout.buildDirectory.file("benchmarks/main/jars/benchmarks.jar")
    argumentProviders.add {
        listOf(
            "-Didea.home.path=${ideaHome.get().asFile.canonicalPath}",
            benchmarkJarPath.get().asFile.toString(),
            "-rf",
            "json",
            "-rff",
            resultFilePath.get().asFile.toString(),
        ) + jmhArgs.map { it.split("\\s".toRegex()) }.orElse(emptyList()).get()
    }
    mainClass.set("-jar")
}
