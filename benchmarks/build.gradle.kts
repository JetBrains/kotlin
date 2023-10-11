import kotlinx.benchmark.gradle.benchmark

val benchmarks_version = "0.3.1"

plugins {
    java
    kotlin("jvm")
    id("org.jetbrains.kotlinx.benchmark") version "0.4.6"
}

dependencies {
    api(kotlinStdlib())
    api(project(":compiler:frontend"))
    api(projectTests(":compiler:tests-common"))
    api(project(":compiler:cli"))
    api(intellijCore())
    api(jpsModel())
    api("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:$benchmarks_version")
}

sourceSets {
    "main" { projectDefault() }
}

benchmark {
    configurations {
        named("main") {
            warmups = 10
            iterations = 10
            iterationTime = 1
            iterationTimeUnit = "sec"
            param("size", 1000)
        }

        register("fir") {
            warmups = 10
            iterations = 10
            iterationTime = 1
            iterationTimeUnit = "sec"
            param("isIR", true)
            param("size", 1000)

            include("CommonCallsBenchmark")
            include("ControlFlowAnalysisBenchmark")
            //include("InferenceBaselineCallsBenchmark")
        }

        register("ni") {
            warmups = 10
            iterations = 10
            iterationTime = 1
            iterationTimeUnit = "sec"
            param("useNI", true)
            param("isIR", false)
            param("size", 1000)
            include("InferenceBaselineCallsBenchmark")
            include("InferenceExplicitArgumentsCallsBenchmark")
            include("InferenceForInApplicableCandidate")
            include("InferenceFromArgumentCallsBenchmark")
            include("InferenceFromReturnTypeCallsBenchmark")
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

val benchmarkTasks = listOf("mainBenchmark", "mainFirBenchmark", "mainNiBenchmark")
tasks.withType<JavaExec>().matching { it.name in benchmarkTasks }.configureEach {
    dependsOn(":createIdeaHomeForTests")
    systemProperty("idea.home.path", ideaHomePathForTests().get().asFile.canonicalPath)
    systemProperty("idea.use.native.fs.for.win", false)
}

tasks.register<JavaExec>("runBenchmark") {
    dependsOn(":createIdeaHomeForTests")

    // jmhArgs example: -PjmhArgs='CommonCalls -p size=500 -p isIR=true -p useNI=true -f 1'
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

    doLast {
        if (project.kotlinBuildProperties.isTeamcityBuild) {
            val jsonArray = com.google.gson.JsonParser.parseString(resultFilePath.get().asFile.readText()).asJsonArray
            jsonArray.forEach {
                val benchmark = it.asJsonObject
                // remove unnecessary name parts from string like this "org.jetbrains.kotlin.benchmarks.CommonCallsBenchmark.benchmark"
                val name = benchmark["benchmark"].asString.removeSuffix(".benchmark").let {
                    val indexOfLastDot = it.indexOfLast { it == '.' }
                    it.removeRange(0..indexOfLastDot)
                }
                val params = benchmark["params"].asJsonObject
                val isIR = if (params.has("isIR")) params["isIR"].asString else "false"
                val useNI = if (params.has("useNI")) params["useNI"].asString else "false"
                val size = params["size"].asString
                val score = "%.3f".format(benchmark["primaryMetric"].asJsonObject["score"].asString.toFloat())

                val irPostfix = if (isIR.toBoolean()) " isIR=true" else ""
                val niPostfix = if (useNI.toBoolean() && !isIR.toBoolean()) " isNI=true" else ""

                println("""##teamcity[buildStatisticValue key='$name size=$size${irPostfix}$niPostfix' value='$score']""")
            }
        }
    }
}
