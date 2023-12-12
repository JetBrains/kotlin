/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:fir:tree"))

    implementation(kotlinxCollectionsImmutable())
    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":compiler:psi"))

    compileOnly(intellijCore())
    compileOnly(libs.guava)

    testCompileOnly(intellijCore())
    testRuntimeOnly(intellijCore())
}

val generatorClasspath: Configuration by configurations.creating
dependencies {
    generatorClasspath(project(":compiler:fir:checkers:checkers-component-generator"))
}
val generateCheckersComponents by tasks.registering(NoDebugJavaExec::class) {
    workingDir = rootDir
    classpath = generatorClasspath
    mainClass.set("org.jetbrains.kotlin.fir.checkers.generator.MainKt")
    systemProperties["line.separator"] = "\n"

    val generationRoot = layout.projectDirectory.dir("gen")
    args(project.name, generationRoot)
    outputs.dir(generationRoot)
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDir(generateCheckersComponents)
    }
    "test" { none() }
}

projectTest(parallel = true) {
    workingDir = rootDir
}
