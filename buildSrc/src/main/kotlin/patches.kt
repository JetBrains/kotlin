/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("UltimateBuildSrc")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ultimate

import org.apache.tools.ant.filters.LineContains
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.creating

// See KT-30178
fun Project.patchFileTemplates(originalPluginJar: Configuration) = tasks.creating(Copy::class) {
    val filteredItems = listOf("#parse(\"File Header.java\")")

    from(zipTree(originalPluginJar.singleFile).matching { include("fileTemplates/**/*.ft") })
    destinationDir = file("$buildDir/$name")

    filter(
         mapOf("negate" to true, "contains" to filteredItems),
         LineContains::class.java
    )

    eachFile {
        logger.kotlinInfo {
            "File \"${this.path}\" in task ${this@patchFileTemplates.path} has been patched to remove lines with the following items: $filteredItems"
        }
    }
}

// Disable `KotlinMPPGradleProjectTaskRunner` in CIDR plugins
fun Project.patchGradleXml(originalPluginJar: Configuration) = tasks.creating(Copy::class) {
    val gradleXmlPath = "META-INF/gradle.xml"
    val filteredItems = listOf("implementation=\"org.jetbrains.kotlin.idea.gradle.execution.KotlinMPPGradleProjectTaskRunner\"")

    from(zipTree(originalPluginJar.singleFile).matching { include(gradleXmlPath) })
    destinationDir = file("$buildDir/$name")

    commentXmlFiles(mapOf(gradleXmlPath to filteredItems))
}
