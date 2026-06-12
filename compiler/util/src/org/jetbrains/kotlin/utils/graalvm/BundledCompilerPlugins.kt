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

/**
 * Bundled plugins, loaded lazily on request. Only expected to be used in the compiler native image,
 * when [isGraalNativeImageRuntime] is `true`. Bundled plugins are listed in [BUNDLED_PLUGIN_DESCRIPTORS_FILE],
 * which is generated in the build process of the native image.
 */
object BundledCompilerPlugins {
    private const val BUNDLED_PLUGIN_DESCRIPTORS_FILE = "META-INF/org/jetbrains/kotlin/bundled-compiler-plugins.txt"
    val pluginInfos: List<BundledPluginInfo> by lazy { loadFromResource() }

    private fun loadFromResource(): List<BundledPluginInfo> {
        val stream = BundledCompilerPlugins::class.java.classLoader.getResourceAsStream(BUNDLED_PLUGIN_DESCRIPTORS_FILE)
            ?: return emptyList()
        return stream.bufferedReader().useLines { lines ->
            lines.mapNotNull { parseLine(it) }.toList()
        }
    }

    private fun parseLine(rawLine: String): BundledPluginInfo? {
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) return null

        val fields = line.split(";").map { it.trim() }
        require(fields.size == 4) {
            "Malformed bundled plugin entry in $BUNDLED_PLUGIN_DESCRIPTORS_FILE (expected 4 ';'-separated fields): $rawLine"
        }
        return BundledPluginInfo(
            pluginId = fields[0],
            pluginRegistrarFqName = fields[1],
            commandLineProcessorFqName = fields[2].ifEmpty { null },
            jarPrefixes = fields[3].split(",").map { it.trim() }.filter { it.isNotEmpty() },
        )
    }

    /**
     * Resolves a single classpath entry to a bundled plugin by matching an [entry] file
     * name against [BundledPluginInfo.jarPrefixes]
     */
    fun lookupByClasspathEntry(entry: String): BundledPluginInfo? {
        val fileName = File(entry).nameWithoutExtension
        return pluginInfos.firstOrNull { info -> info.jarPrefixes.any { fileName.startsWith(it) } }
    }
}
