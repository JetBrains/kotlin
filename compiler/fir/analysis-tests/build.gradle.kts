import org.jetbrains.kotlin.ideaExt.idea

/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "guava", rootProject = rootProject) }

    testApi(intellijDep())
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(project(":compiler:cli"))
    testApi(project(":compiler:fir:checkers"))
    testApi(project(":compiler:fir:entrypoint"))
    testApi(project(":compiler:frontend"))

    testApi(platform("org.junit:junit-bom:5.7.0"))
    testApi("org.junit.jupiter:junit-jupiter")
    testApi("org.junit.platform:junit-platform-commons:1.7.0")

    testCompileOnly(project(":kotlin-reflect-api"))
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":core:descriptors.runtime"))

    testImplementation(intellijCoreDep()) { includeJars("intellij-core") }
    testImplementation(intellijDep()) {
        // This dependency is needed only for FileComparisonFailure
        includeJars("idea_rt", rootProject = rootProject)
        isTransitive = false
    }

    // This is needed only for using FileComparisonFailure, which relies on JUnit 3 classes
    testRuntimeOnly(commonDep("junit:junit"))
    testRuntimeOnly(intellijDep()) {
        includeJars(
            "jps-model",
            "extensions",
            "util",
            "platform-api",
            "platform-impl",
            "idea",
            "guava",
            "trove4j",
            "asm-all",
            "log4j",
            "jdom",
            "streamex",
            "bootstrap",
            "jna",
            rootProject = rootProject
        )
    }

    Platform[202] {
        testRuntimeOnly(intellijDep()) { includeJars("intellij-deps-fastutil-8.3.1-1") }
    }
    Platform[203].orHigher {
        testRuntimeOnly(intellijDep()) { includeJars("intellij-deps-fastutil-8.3.1-3") }
    }
    testRuntimeOnly(toolsJar())
}

val generationRoot = projectDir.resolve("tests-gen")

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        this.java.srcDir(generationRoot.name)
    }
}

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(plugin = "idea")
    idea {
        this.module.generatedSourceDirs.add(generationRoot)
    }
}

projectTest(parallel = true, jUnit5Enabled = true) {
    dependsOn(":dist")
    workingDir = rootDir
    jvmArgs!!.removeIf { it.contains("-Xmx") }
    maxHeapSize = "3g"

    useJUnitPlatform()
}

testsJar()
