import kotlinx.benchmark.gradle.benchmark

val benchmarks_version = "0.2.0-dev-4"
buildscript {
    val benchmarks_version = "0.2.0-dev-4"

    repositories {
        val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true
        if (cacheRedirectorEnabled) {
            maven("https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlinx")
            maven("https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-dev")
        } else {
            maven("https://dl.bintray.com/kotlin/kotlinx")
            maven("https://dl.bintray.com/kotlin/kotlin-dev")
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
        maven("https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-dev")
    } else {
        maven("https://dl.bintray.com/kotlin/kotlinx")
        maven("https://dl.bintray.com/kotlin/kotlin-dev")
    }
}

dependencies {
    compile(kotlinStdlib())
    compile(project(":compiler:frontend"))
    compile(project(":compiler:cli"))
    compile(intellijCoreDep()) { includeJars("intellij-core") }
    compile(jpsStandalone()) { includeJars("jps-model") }
    Platform[192].orHigher {
        compile(intellijPluginDep("java"))
    }
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
            //include("InferenceBaselineCallsBenchmark")
        }
    }
    targets {
        register("main")
    }
}
