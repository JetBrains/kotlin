/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:frontend.common"))
    compile(project(":core:descriptors"))
    compile(project(":compiler:fir:cones"))
    compile(project(":compiler:ir.tree"))
    // Necessary only to store bound PsiElement inside FirElement
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDir("visitors")
    }
}

val generatorClasspath by configurations.creating

dependencies {
    generatorClasspath(project("visitors-generator"))
}

val generateVisitors by tasks.creating(NoDebugJavaExec::class) {
    val generationRoot = "$projectDir/src/org/jetbrains/kotlin/fir/"
    val output = "$projectDir/visitors"

    val allSourceFiles = fileTree(generationRoot) {
        include("**/*.kt")
    }

    inputs.files(allSourceFiles)
    outputs.files(output)

    classpath = generatorClasspath
    args(generationRoot, output)
    main = "org.jetbrains.kotlin.fir.visitors.generator.VisitorsGeneratorKt"
}

val compileKotlin by tasks

compileKotlin.dependsOn(generateVisitors)
