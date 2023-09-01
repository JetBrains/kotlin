group = "org.jetbrains"
version = "1.0-SNAPSHOT"

plugins {
    java
    application
    kotlin("jvm") version "1.9.10"
}

repositories {
    maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:kotlin-build-benchmarks:1.0-SNAPSHOT")
    implementation(kotlin("stdlib"))
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass = "RunBenchmarksKt"
}

tasks.register("runGavra0", JavaExec::class) {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "RunGavra0BenchmarksKt"
}

tasks.register("runAbiSnapshot", JavaExec::class) {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "RunAbiSnapshotsBenchmarksKt"
}