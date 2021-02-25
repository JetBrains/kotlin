
description = "Kotlin IDE Performance Tests"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testRuntimeOnly(intellijDep())
    testRuntimeOnly(intellijRuntimeAnnotations())
    testRuntimeOnly(toolsJar())
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":plugins:android-extensions-ide"))
    testRuntimeOnly(project(":plugins:kapt3-idea"))
    testRuntimeOnly(project(":sam-with-receiver-ide-plugin"))
    testRuntimeOnly(project(":noarg-ide-plugin"))
    testRuntimeOnly(project(":allopen-ide-plugin"))
    testRuntimeOnly(project(":kotlin-scripting-idea"))
    testRuntimeOnly(project(":kotlinx-serialization-ide-plugin"))
    testRuntimeOnly(project(":plugins:parcelize:parcelize-ide"))
    testRuntimeOnly(project(":nj2k:nj2k-services"))
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":idea:kotlin-gradle-tooling"))
    testRuntimeOnly(project(":kotlin-gradle-statistics"))

    testImplementation(intellijPluginDep("gradle"))
    testImplementation(project(":compiler:backend"))
    testImplementation(project(":idea:idea-jvm"))
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":idea:idea-test-framework")) { isTransitive = false }
    testImplementation(projectTests(":idea"))
    testImplementation(project(":idea:idea-gradle")) { isTransitive = false }
    testImplementation(commonDep("junit:junit"))
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.11.4")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.4")
    testImplementation("khttp:khttp:1.0.0")

    testCompileOnly(intellijPluginDep("java"))
    testRuntimeOnly(intellijPluginDep("java"))

    testImplementation(intellijPluginDep("gradle-java"))
    testRuntimeOnly(intellijPluginDep("gradle-java"))

    testCompileOnly(intellijDep())
    testCompileOnly(project(":nj2k"))
    testCompileOnly(intellijDep()) { includeJars("slf4j-api-1.7.25") }
}

sourceSets {
    "test" { projectDefault() }
}

runtimeJar()

sourcesJar()

javadocJar()

apply(from = "$rootDir/gradle/kotlinPluginPublication.gradle.kts")

projectTest(taskName = "performanceTest") {
    exclude("**/*WholeProjectPerformanceTest*")

    val currentOs = org.gradle.internal.os.OperatingSystem.current()

    if (!currentOs.isWindows) {
        System.getenv("ASYNC_PROFILER_HOME")?.let { asyncProfilerHome ->
            classpath += files("$asyncProfilerHome/build/async-profiler.jar")
        }
    }

    workingDir = rootDir

    jvmArgs?.removeAll { it.startsWith("-Xmx") }

    maxHeapSize = "3g"
    jvmArgs("-Didea.debug.mode=true")
    jvmArgs("-XX:SoftRefLRUPolicyMSPerMB=50")

    jvmArgs(
        "-XX:+UseCompressedOops",
        "-Didea.ProcessCanceledException=disabled",
        "-XX:+UseConcMarkSweepGC"
    )

    System.getenv("YOURKIT_PROFILER_HOME")?.let {yourKitHome ->
        when {
            currentOs.isLinux -> {
                jvmArgs("-agentpath:$yourKitHome/bin/linux-x86-64/libyjpagent.so")
                classpath += files("$yourKitHome/lib/yjp-controller-api-redist.jar")
            }
            currentOs.isMacOsX -> {
                jvmArgs("-agentpath:$yourKitHome/Contents/Resources/bin/mac/libyjpagent.dylib=delay=5000,_socket_timeout_ms=120000,disablealloc,disable_async_sampling,disablenatives")
                classpath += files("$yourKitHome/Contents/Resources/lib/yjp-controller-api-redist.jar")
            }
        }
    }

    doFirst {
        systemProperty("idea.home.path", intellijRootDir().canonicalPath)
        project.findProperty("cacheRedirectorEnabled")?.let {
            systemProperty("kotlin.test.gradle.import.arguments", "-PcacheRedirectorEnabled=$it")
        }
    }
}

projectTest(taskName = "wholeProjectsPerformanceTest") {
    exclude(
        "**/*Generated*",
        "**/*PerformanceProjectsTest*",
        "**/*PerformanceStressTest*",
        "**/*PerformanceNativeProjectsTest*"
    )
    include("**/*WholeProjectPerformanceTest*")

    workingDir = rootDir

    jvmArgs?.removeAll { it.startsWith("-Xmx") }

    maxHeapSize = "3g"
    jvmArgs("-DperformanceProjects=${System.getProperty("performanceProjects")}")
    jvmArgs("-Didea.debug.mode=true")
    jvmArgs("-DemptyProfile=${System.getProperty("emptyProfile")}")
    jvmArgs("-XX:SoftRefLRUPolicyMSPerMB=50")
    jvmArgs(
        "-XX:+UseCompressedOops",
        "-XX:+UseConcMarkSweepGC"
    )

    doFirst {
        systemProperty("idea.home.path", intellijRootDir().canonicalPath)
        project.findProperty("cacheRedirectorEnabled")?.let {
            systemProperty("kotlin.test.gradle.import.arguments", "-PcacheRedirectorEnabled=$it")
        }
    }
}

task("aggregateResults", JavaExec::class) {
    dependsOn(":idea:performanceTests:performanceTest")

    main = "org.jetbrains.kotlin.idea.perf.util.AggregateResultsKt"
    classpath = sourceSets["test"].runtimeClasspath
    workingDir = rootDir
    args(listOf(File(rootDir, "build")))
}


testsJar()