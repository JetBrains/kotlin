
description = "Kotlin IDE Performance Tests"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testRuntimeOnly(intellijDep())
    testRuntimeOnly(intellijRuntimeAnnotations())
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":plugins:android-extensions-ide"))
    testRuntimeOnly(project(":plugins:kapt3-idea"))
    testRuntimeOnly(project(":sam-with-receiver-ide-plugin"))
    testRuntimeOnly(project(":noarg-ide-plugin"))
    testRuntimeOnly(project(":allopen-ide-plugin"))
    testRuntimeOnly(project(":kotlin-scripting-idea"))
    testRuntimeOnly(project(":kotlinx-serialization-ide-plugin"))
    testRuntimeOnly(project(":nj2k:nj2k-services"))
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":idea:kotlin-gradle-tooling"))
    testRuntimeOnly(project(":idea:idea-gradle-tooling-api"))

    testImplementation(intellijPluginDep("gradle"))
    testImplementation(project(":compiler:backend"))
    testImplementation(project(":idea:idea-jvm"))
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":idea:idea-test-framework")) { isTransitive = false }
    testImplementation(projectTests(":idea"))
    testImplementation(commonDep("junit:junit"))

    Platform[192].orHigher {
        testCompileOnly(intellijPluginDep("java"))
        testRuntimeOnly(intellijPluginDep("java"))
    }

    testCompileOnly(intellijDep())
    testCompileOnly(project(":nj2k"))
    testCompileOnly(project(":idea:idea-gradle-tooling-api"))
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
    classpath += files("${System.getenv("ASYNC_PROFILER_HOME")}/build/async-profiler.jar")
    workingDir = rootDir

    jvmArgs?.removeAll { it.startsWith("-Xmx") }

    maxHeapSize = "3g"
    jvmArgs("-Didea.debug.mode=true")
    jvmArgs("-XX:SoftRefLRUPolicyMSPerMB=50")
    jvmArgs(
        "-XX:ReservedCodeCacheSize=240m",
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
