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
    compile(project(":core:descriptors"))
    compile(project(":core:deserialization"))
    compile(project(":compiler:fir:cones"))
    compile(project(":compiler:fir:tree"))
    compile(project(":compiler:fir:resolve"))
    compile(project(":compiler:fir:java"))
    compile(project(":compiler:cli"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("commons-lang-2.4") }

    compile("org.jetbrains.kotlinx:kotlinx-html-jvm:0.6.12")
}

sourceSets {
    "main" { projectDefault() }
}


testsJar()