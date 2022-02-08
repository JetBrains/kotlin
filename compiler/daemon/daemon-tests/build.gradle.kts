
description = "Kotlin Daemon Tests"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val ktorExcludesForDaemon: List<Pair<String, String>> by rootProject.extra

dependencies {
    testApi(project(":kotlin-test:kotlin-test-jvm"))
    testApi(kotlinStdlib())
    testApi(commonDependency("junit:junit"))
    testCompileOnly(project(":kotlin-test:kotlin-test-jvm"))
    testCompileOnly(project(":kotlin-test:kotlin-test-junit"))
    testApi(project(":kotlin-daemon-client"))
    testApi(project(":kotlin-daemon-client-new"))
    testCompileOnly(project(":kotlin-daemon"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(commonDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }
    testApi(commonDependency("io.ktor", "ktor-network")) {
        ktorExcludesForDaemon.forEach { (group, module) ->
            exclude(group = group, module = module)
        }
    }
    testImplementation(project(":kotlin-daemon"))
    testImplementation(intellijCore())
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
