/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    // We explicitly configure versions of plugins in settings.gradle.kts.
    // due to https://github.com/gradle/gradle/issues/1697.
    id("kotlin")
    id("kotlinx-serialization")
    groovy
    `java-gradle-plugin`
}

val rootProperties = Properties().apply {
    rootDir.resolve("../gradle.properties").reader().use(::load)
}

val kotlinVersion: String by rootProperties
val kotlinCompilerRepo: String by rootProperties
val buildKotlinVersion: String by rootProperties
val buildKotlinCompilerRepo: String by rootProperties
val konanVersion: String by rootProperties

group = "org.jetbrains.kotlin"
version = konanVersion

repositories {
    maven(kotlinCompilerRepo)
    maven(buildKotlinCompilerRepo)
    maven("https://cache-redirector.jetbrains.com/maven-central")
    maven("https://kotlin.bintray.com/kotlinx")
}

dependencies {
    compileOnly(gradleApi())

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("com.ullink.slack:simpleslackapi:1.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.10.0")

    implementation("io.ktor:ktor-client-auth:1.2.1")
    implementation("io.ktor:ktor-client-core:1.2.1")
    implementation("io.ktor:ktor-client-cio:1.2.1")

    api("org.jetbrains.kotlin:kotlin-native-utils:$kotlinVersion")

    // Located in <repo root>/shared and always provided by the composite build.
    api("org.jetbrains.kotlin:kotlin-native-shared:$konanVersion")
}

sourceSets["main"].withConvention(KotlinSourceSet::class) {
    kotlin.srcDir("$projectDir/../tools/benchmarks/shared/src")
}

gradlePlugin {
    plugins {
        create("benchmarkPlugin") {
            id = "benchmarking"
            implementationClass = "org.jetbrains.kotlin.benchmark.KotlinNativeBenchmarkingPlugin"
        }
        create("compileBenchmarking") {
            id = "compile-benchmarking"
            implementationClass = "org.jetbrains.kotlin.benchmark.CompileBenchmarkingPlugin"
        }
        create("swiftBenchmarking") {
            id = "swift-benchmarking"
            implementationClass = "org.jetbrains.kotlin.benchmark.SwiftBenchmarkingPlugin"
        }
    }
}

val compileKotlin: KotlinCompile by tasks
val compileGroovy: GroovyCompile by tasks

// Add Kotlin classes to a classpath for the Groovy compiler
compileGroovy.apply {
    classpath += project.files(compileKotlin.destinationDir)
    dependsOn(compileKotlin)
}