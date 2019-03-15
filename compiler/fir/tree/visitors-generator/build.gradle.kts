/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

import tasks.WriteCopyrightToFile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:psi"))

    compile(intellijCoreDep()) { includeJars("intellij-core") }
    compile(intellijDep()) {
        includeJars("trove4j", "picocontainer", rootProject = rootProject)
        isTransitive = false
    }
    compile(intellijDep()) { includeJars("guava", rootProject = rootProject) }
}

val writeCopyright by task<WriteCopyrightToFile> {
    outputFile = file("$buildDir/copyright/notice.txt")
    commented = true
}

val processResources by tasks
processResources.dependsOn(writeCopyright)

sourceSets {
    "main" {
        projectDefault()
        resources.srcDir("$buildDir/copyright")
    }
    "test" {}
}

