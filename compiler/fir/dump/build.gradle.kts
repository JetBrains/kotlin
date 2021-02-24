/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

repositories {
    maven { setUrl("https://dl.bintray.com/kotlin/kotlinx.html/") }
}

dependencies {
    implementation(project(":core:descriptors"))
    implementation(project(":core:deserialization"))
    implementation(project(":compiler:fir:cones"))
    implementation(project(":compiler:fir:tree"))
    implementation(project(":compiler:fir:resolve"))
    implementation(project(":compiler:fir:java"))
    implementation(project(":compiler:cli"))

    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.6.12")

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("commons-lang-2.4") }

}

sourceSets {
    "main" { projectDefault() }
}

testsJar()
