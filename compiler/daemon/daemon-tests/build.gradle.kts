
description = "Kotlin Daemon Tests"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val ktorExcludesForDaemon: List<Pair<String, String>> by rootProject.extra

dependencies {
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(kotlinStdlib())
    testCompile(commonDep("junit:junit"))
    testCompileOnly(project(":kotlin-test:kotlin-test-jvm"))
    testCompileOnly(project(":kotlin-test:kotlin-test-junit"))
    testCompile(projectRuntimeJar(":kotlin-daemon-client"))
    testCompile(projectRuntimeJar(":kotlin-daemon-client-new"))
    testCompileOnly(project(":kotlin-daemon"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }
    testCompile(commonDep("io.ktor", "ktor-network")) {
        ktorExcludesForDaemon.forEach { (group, module) ->
            exclude(group = group, module = module)
        }
    }
    testRuntime(project(":kotlin-daemon"))
    testRuntime(intellijCoreDep()) { includeJars("intellij-core") }
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
    systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
}
