plugins {
    java
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

val ktorExcludesForDaemon: List<Pair<String, String>> by rootProject.extra

dependencies {
    compileOnly(project(":daemon-common"))
    compile(kotlinStdlib())
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeIntellijCoreJarDependencies(project) }
    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) {
        isTransitive = false
    }
    compile(commonDep("io.ktor", "ktor-network")) {
        ktorExcludesForDaemon.forEach { (group, module) ->
            exclude(group = group, module = module)
        }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
