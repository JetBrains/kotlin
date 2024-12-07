/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":core:descriptors"))
    implementation(project(":core:deserialization"))
    implementation(project(":compiler:fir:cones"))
    implementation(project(":compiler:fir:tree"))
    implementation(project(":compiler:fir:providers"))
    implementation(project(":compiler:fir:semantics"))
    implementation(project(":compiler:fir:resolve"))
    implementation(project(":compiler:fir:java"))
    implementation(project(":compiler:cli"))

    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.3")

    compileOnly(intellijCore())
    compileOnly(commonDependency("commons-lang:commons-lang"))
}

sourceSets {
    "main" { projectDefault() }
}

testsJar()
