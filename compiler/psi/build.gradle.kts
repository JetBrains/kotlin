/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


plugins {
    kotlin("jvm")
    id("jps-compatible")
}

repositories {
    maven("https://jetbrains.bintray.com/intellij-third-party-dependencies")
}

val jflexPath by configurations.creating

dependencies {
    val compile by configurations
    val compileOnly by configurations

    compile(project(":core:descriptors"))
    compile(project(":compiler:util"))
    compile(project(":kotlin-script-runtime"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("guava", "trove4j", rootProject = rootProject) }

    jflexPath(commonDep("org.jetbrains.intellij.deps.jflex", "jflex"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}



ant.importBuild("buildLexer.xml")

ant.properties["builddir"] = buildDir.absolutePath

tasks.findByName("lexer")!!.apply {
    doFirst {
        ant.properties["flex.classpath"] = jflexPath.asPath
    }
}

