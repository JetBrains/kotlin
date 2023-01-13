description = "Kotlin Daemon"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val ktorExcludesForDaemon : List<Pair<String, String>> by rootProject.extra

dependencies {
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:cli-js"))
    compileOnly(project(":compiler:incremental-compilation-impl"))

    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:trove4j"))

    runtimeOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    embedded(project(":daemon-common")) { isTransitive = false }
    api(commonDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) {
        isTransitive = false
    }
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

publish()

runtimeJar()

sourcesJar()

javadocJar()

tasks {
    val compileKotlin by existing(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
        kotlinOptions {
            freeCompilerArgs += "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi"
        }
    }
}
