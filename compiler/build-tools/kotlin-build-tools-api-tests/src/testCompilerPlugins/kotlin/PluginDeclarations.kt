/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPluginOption
import java.io.File
import java.nio.file.Paths

private enum class KnownCompilerPlugin(val pluginId: String, val classpathSystemPropertyName: String) {
    NOARG("org.jetbrains.kotlin.noarg", "NOARG_COMPILER_PLUGIN"),
    ASSIGNMENT("org.jetbrains.kotlin.assignment", "ASSIGNMENT_COMPILER_PLUGIN"),
}

private fun getCompilerPlugin(plugin: KnownCompilerPlugin, arguments: List<CompilerPluginOption>): CompilerPlugin {
    val classpath = System.getProperty(plugin.classpathSystemPropertyName).split(File.pathSeparator).map { Paths.get(it) }
    return CompilerPlugin(
        pluginId = plugin.pluginId,
        classpath = classpath,
        rawArguments = arguments,
        orderingRequirements = emptySet(),
    )
}

internal val NOARG_PLUGIN =
    getCompilerPlugin(KnownCompilerPlugin.NOARG, listOf(CompilerPluginOption("annotation", "GenerateNoArgsConstructor")))
internal val ASSIGNMENT_PLUGIN =
    getCompilerPlugin(KnownCompilerPlugin.ASSIGNMENT, listOf(CompilerPluginOption("annotation", "GenerateAssignment")))