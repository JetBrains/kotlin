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
import org.gradle.kotlin.dsl.creating

// See KT-30178
fun Project.patchFileTemplates(originalPluginJar: Configuration) = tasks.creating {
    val filteredItems = listOf("#parse(\"File Header.java\")")

    inputs.files(originalPluginJar)
    outputs.dir("$buildDir/$name")

    doFirst {
        copy {
            from(zipTree(inputs.files.singleFile).matching { include("fileTemplates/**/*.ft") })
            into(outputs.files.singleFile)
            filter(
                    mapOf("negate" to true, "contains" to filteredItems),
                    LineContains::class.java
            )
        }
    }
}
