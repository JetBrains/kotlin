description = "Kotlin Daemon"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:incremental-compilation-impl"))
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:trove4j"))

    runtimeOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    embedded(project(":daemon-common")) { isTransitive = false }
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
