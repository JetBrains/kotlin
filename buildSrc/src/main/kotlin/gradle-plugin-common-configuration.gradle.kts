/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

publishGradlePlugin()
standardPublicJars()

extensions.extraProperties["kotlin.stdlib.default.dependency"] = "false"

dependencies {
    compileOnly(kotlinStdlib())
    compileOnly(gradleApi())
}

// These dependencies will be provided by Gradle and we should prevent version conflict
fun Configuration.excludeGradleCommonDependencies() {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-script-runtime")
}
configurations {
    "implementation" {
        excludeGradleCommonDependencies()
    }
    "api" {
        excludeGradleCommonDependencies()
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.languageVersion = "1.4"
    kotlinOptions.apiVersion = "1.4"
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xskip-prerelease-check",
        "-Xsuppress-version-warnings",
        "-Xuse-ir" // Needed as long as languageVersion is less than 1.5.
    )
}

tasks.named<Jar>("jar") {
    callGroovy("manifestAttributes", manifest, project)
}
