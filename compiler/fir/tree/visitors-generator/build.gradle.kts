import tasks.WriteCopyrightToFile

/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */


apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

jvmTarget = "1.6"

dependencies {
    val compile by configurations

    compile(project(":compiler:psi"))

    compile(intellijCoreDep()) { includeJars("intellij-core") }
    compile(intellijDep()) {
        includeJars("trove4j", "picocontainer-1.2", "annotations", rootProject = rootProject)
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

