import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Kotlin Daemon"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val ktorExcludesForDaemon : List<Pair<String, String>> by rootProject.extra

dependencies {
    compile(commonDep("org.fusesource.jansi", "jansi"))
    compile(commonDep("org.jline", "jline"))

    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:cli-js"))
    compileOnly(project(":compiler:incremental-compilation-impl"))
    compileOnly(project(":daemon-common-new"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("trove4j") }

    runtime(project(":kotlin-reflect"))

    embedded(project(":daemon-common")) { isTransitive = false }
    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) {
        isTransitive = false
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

publish()

runtimeJar()

sourcesJar()

javadocJar()
