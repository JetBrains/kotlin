group = "org.jetbrains"
version = "1.0-SNAPSHOT"

plugins {
    java
    application
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.0-rc")
    }
}

apply {
    plugin("kotlin")
}

repositories {
    maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
    mavenLocal()
}

dependencies {
    implementation("org.jetbrains:kotlin-build-benchmarks:1.0-SNAPSHOT")
}

application {
    mainClassName = "RunBenchmarksKt"
}

tasks.register("runGavra0", JavaExec::class) {
    classpath = sourceSets.main.get().runtimeClasspath
    main = "RunGavra0BenchmarksKt"
}