/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("UltimateBuildSrc")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ultimate

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.kotlin.dsl.*

fun Project.defaultSourceSets() {
    sourceSets.maybeCreate("main").apply {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }

    sourceSets.maybeCreate("test").apply {
        java.setSrcDirs(listOf("test", "tests"))
        resources.setSrcDirs(emptyList<String>())
    }
}

internal val Project.mainSourceSetOutput: SourceSetOutput
    get() = sourceSets.mainSourceSetOutput

private val Project.sourceSets: SourceSetContainer
    get() = the<JavaPluginConvention>().sourceSets

private val SourceSetContainer.mainSourceSetOutput: SourceSetOutput
    get() = getByName("main").output
