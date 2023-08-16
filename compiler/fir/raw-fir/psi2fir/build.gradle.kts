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
    api(project(":compiler:fir:raw-fir:raw-fir.common"))
    implementation(project(":compiler:psi"))
    implementation(kotlinxCollectionsImmutable())

    compileOnly(intellijCore())
    compileOnly(libs.guava)

    testImplementation(libs.junit4)
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    testCompileOnly(project(":kotlin-test:kotlin-test-jvm"))
    testCompileOnly(project(":kotlin-test:kotlin-test-junit"))

    testRuntimeOnly(project(":core:descriptors.runtime"))

    testCompileOnly(intellijCore())
    testRuntimeOnly(intellijCore())
}

val generationRoot = projectDir.resolve("tests-gen")

sourceSets {
    "main" { projectDefault() }
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

projectTest(parallel = true) {
    workingDir = rootDir
}

testsJar()
