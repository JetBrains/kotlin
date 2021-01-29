import kotlinx.benchmark.gradle.benchmark

val benchmarks_version = "0.2.0-dev-7"
buildscript {
    val benchmarks_version = "0.2.0-dev-7"

    repositories {
        val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true
        if (cacheRedirectorEnabled) {
            maven("https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlinx")
        } else {
            maven("https://dl.bintray.com/kotlin/kotlinx")
        }
    }
    dependencies {
        classpath("org.jetbrains.kotlinx:kotlinx.benchmark.gradle:$benchmarks_version")
    }
}

apply(plugin = "kotlinx.benchmark")

plugins {
    java
    kotlin("jvm")
}

repositories {
    val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true
    if (cacheRedirectorEnabled) {
        maven("https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlinx")
   } else {
        maven("https://dl.bintray.com/kotlin/kotlinx")
    }
}

dependencies {
    compile(kotlinStdlib())
    compile(project(":compiler:frontend"))
    compile(projectTests(":compiler:tests-common"))
    compile(project(":compiler:cli"))
    compile(intellijCoreDep()) { includeJars("intellij-core") }
    compile(jpsStandalone()) { includeJars("jps-model") }
    compile(intellijPluginDep("java"))
    compile(intellijDep()) { includeIntellijCoreJarDependencies(project) }
    compile("org.jetbrains.kotlinx:kotlinx.benchmark.runtime-jvm:$benchmarks_version")
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

tasks.matching { it is Zip && it.name == "mainBenchmarkJar" }.configureEach {
    this as Zip
    isZip64 = true
    archiveFileName.set("benchmarks.jar")
}

val benchmarkTasks = listOf("mainBenchmark", "mainFirBenchmark", "mainNiBenchmark")
tasks.matching { it is JavaExec && it.name in benchmarkTasks }.configureEach {
    this as JavaExec
    systemProperty("idea.home.path", intellijRootDir().canonicalPath)
}

tasks.register<JavaExec>("runBenchmark") {
    // jmhArgs example: -PjmhArgs='CommonCalls -p size=500 -p isIR=true -p useNI=true -f 1'
    val jmhArgs = if (project.hasProperty("jmhArgs")) project.property("jmhArgs").toString() else ""
    val resultFilePath = "$buildDir/benchmarks/jmh-result.json"
    val ideaHome = intellijRootDir().canonicalPath

    val benchmarkJarPath = "$buildDir/benchmarks/main/jars/benchmarks.jar"
    args = mutableListOf("-Didea.home.path=$ideaHome", benchmarkJarPath, "-rf", "json", "-rff", resultFilePath) + jmhArgs.split("\\s".toRegex())
    main = "-jar"

    doLast {
        if (project.kotlinBuildProperties.isTeamcityBuild) {
            val jsonArray = com.google.gson.JsonParser.parseString(File(resultFilePath).readText()).asJsonArray
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
