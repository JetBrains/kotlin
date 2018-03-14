/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

jvmTarget = "1.6"

dependencies {
    val compile by configurations
    val compileOnly by configurations

    compile(project(":compiler:psi"))
    compile(project(":core:descriptors"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "annotations") }
}


sourceSets {
    "main" {
        projectDefault()
        java.srcDir("visitors")
    }
    "test" {}
}

val generatorClasspath by configurations.creating

dependencies {
    generatorClasspath(project("visitors-generator"))
}

val generateVisitors by tasks.creating(JavaExec::class) {
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
