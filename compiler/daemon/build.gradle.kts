description = "Kotlin Daemon"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:incremental-compilation-impl"))
    compileOnly(intellijCore())
    compileOnly(libs.intellij.fastutil)
    compileOnly(libs.guava)

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

optInToK1Deprecation()
