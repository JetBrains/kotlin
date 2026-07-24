/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils.graalvm

import java.io.File

/**
 * Information about a compiler plugin that is bundled into the compiler. Used in the context of the GraalVM native
 * image: these plugins are included in the native image and are loaded reflectively when the user requests them via
 * either `-Xplugin/-Xcompiler-plugin`.
 */
data class BundledPluginInfo(
    val pluginId: String,
    val pluginRegistrarFqName: String,
    val commandLineProcessorFqName: String?,
    val jarPrefixes: List<String>,
)

object BundledCompilerPlugins {
    val pluginInfos = listOf(
        BundledPluginInfo(
            pluginId = "org.jetbrains.kotlinx.serialization",
            pluginRegistrarFqName = "org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationComponentRegistrar",
            commandLineProcessorFqName = "org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginOptions",
            jarPrefixes = listOf(
                "kotlin-serialization-compiler-plugin",
            ),
        ),
        BundledPluginInfo(
            pluginId = "org.jetbrains.kotlin.allopen",
            pluginRegistrarFqName = "org.jetbrains.kotlin.allopen.AllOpenComponentRegistrar",
            commandLineProcessorFqName = "org.jetbrains.kotlin.allopen.AllOpenCommandLineProcessor",
            jarPrefixes = listOf(
                "allopen-compiler-plugin",
                "kotlin-allopen-compiler-plugin",
            ),
        ),
        BundledPluginInfo(
            pluginId = "org.jetbrains.kotlin.noarg",
            pluginRegistrarFqName = "org.jetbrains.kotlin.noarg.NoArgComponentRegistrar",
            commandLineProcessorFqName = "org.jetbrains.kotlin.noarg.NoArgCommandLineProcessor",
            jarPrefixes = listOf(
                "noarg-compiler-plugin",
                "kotlin-noarg-compiler-plugin",
            ),
        ),
        BundledPluginInfo(
            pluginId = "org.jetbrains.kotlin.samWithReceiver",
            pluginRegistrarFqName = "org.jetbrains.kotlin.samWithReceiver.SamWithReceiverComponentRegistrar",
            commandLineProcessorFqName = "org.jetbrains.kotlin.samWithReceiver.SamWithReceiverCommandLineProcessor",
            jarPrefixes = listOf(
                "sam-with-receiver-compiler-plugin",
                "kotlin-sam-with-receiver-compiler-plugin",
            ),
        ),
        BundledPluginInfo(
            pluginId = "org.jetbrains.kotlin.assignment",
            pluginRegistrarFqName = "org.jetbrains.kotlin.assignment.plugin.AssignmentComponentRegistrar",
            commandLineProcessorFqName = "org.jetbrains.kotlin.assignment.plugin.AssignmentCommandLineProcessor",
            jarPrefixes = listOf(
                "assignment-compiler-plugin",
                "kotlin-assignment-compiler-plugin",
            ),
        ),
        BundledPluginInfo(
            pluginId = "org.jetbrains.kotlin.lombok",
            pluginRegistrarFqName = "org.jetbrains.kotlin.lombok.LombokComponentRegistrar",
            commandLineProcessorFqName = "org.jetbrains.kotlin.lombok.LombokCommandLineProcessor",
            jarPrefixes = listOf(
                "lombok-compiler-plugin",
                "kotlin-lombok-compiler-plugin",
            ),
        ),
        BundledPluginInfo(
            pluginId = "org.jetbrains.kotlin.powerassert",
            pluginRegistrarFqName = "org.jetbrains.kotlin.powerassert.PowerAssertCompilerPluginRegistrar",
            commandLineProcessorFqName = "org.jetbrains.kotlin.powerassert.PowerAssertCommandLineProcessor",
            jarPrefixes = listOf(
                "power-assert-compiler-plugin",
                "kotlin-power-assert-compiler-plugin",
            ),
        ),
        BundledPluginInfo(
            pluginId = "androidx.compose.compiler.plugins.kotlin",
            pluginRegistrarFqName = "androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar",
            commandLineProcessorFqName = "androidx.compose.compiler.plugins.kotlin.ComposeCommandLineProcessor",
            jarPrefixes = listOf(
                "compose-compiler-plugin",
                "kotlin-compose-compiler-plugin",
            ),
        ),
    )

    /**
     * Resolves a single classpath entry to a bundled plugin by matching an [entry] file
     * name against [BundledPluginInfo.jarPrefixes]
     */
    fun lookupByClasspathEntry(entry: String): BundledPluginInfo? {
        val fileName = File(entry).nameWithoutExtension
        return pluginInfos.firstOrNull { info -> info.jarPrefixes.any { fileName.startsWith(it) } }
    }
}
