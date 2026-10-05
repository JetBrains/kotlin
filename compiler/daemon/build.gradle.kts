description = "Kotlin Daemon"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.protobuf)
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:incremental-compilation-impl"))
    compileOnly(intellijCore())
    compileOnly(libs.intellij.fastutil)
    compileOnly(libs.guava)
    compileOnly(project(":compiler:build-tools:kotlin-build-tools-impl"))

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
