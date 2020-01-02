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
        this.java.srcDir("gen")
    }
}

val generatorClasspath by configurations.creating

dependencies {
    generatorClasspath(project("tree-generator"))
}

val generationRoot = projectDir.resolve("gen")

val generateTree by tasks.registering(NoDebugJavaExec::class) {

    val generatorRoot = "$projectDir/tree-generator/src/"

    val generatorConfigurationFiles = fileTree(generatorRoot) {
        include("**/*.kt")
    }

    inputs.files(generatorConfigurationFiles)
    outputs.dirs(generationRoot)

    args(generationRoot)
    classpath = generatorClasspath
    main = "org.jetbrains.kotlin.fir.tree.generator.MainKt"
    systemProperties["line.separator"] = "\n"
}

val compileKotlin by tasks

compileKotlin.dependsOn(generateTree)

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(plugin = "idea")
    idea {
        this.module.generatedSourceDirs.add(generationRoot)
    }
}
