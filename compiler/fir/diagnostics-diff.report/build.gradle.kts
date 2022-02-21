/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    id("jps-compatible")
    application
}

dependencies {
    implementation(project(":kotlin-stdlib"))
    implementation(project(":compiler:fir:diagnostics-diff.common"))

    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.2")
}

val appName = "diagnostics-diff-report"

application {
    mainClass.set("org.jetbrains.kotlin.fir.diagnostics.diff.MainKt")
    applicationName = appName
}

tasks.withType<CreateStartScripts> {
    applicationName = appName
}

sourceSets {
    "main" { projectDefault() }
}
