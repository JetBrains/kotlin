/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPluginPartialOrderRelation
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import kotlin.io.path.absolutePathString

internal fun CommonCompilerArguments.applyCompilerPlugins(plugins: List<CompilerPlugin>) {
    val filteredPlugins = plugins.filter { it.pluginId != RAW_PLUGIN_ID }
    validatePluginsConfiguration(filteredPlugins)
    pluginClasspaths =
        (pluginClasspaths ?: emptyArray()) + filteredPlugins.flatMap { it.classpath }.map { it.absolutePathString() }.toTypedArray()
    pluginOptions = (pluginOptions
        ?: emptyArray()) + filteredPlugins.flatMap { plugin -> plugin.rawArguments.map { option -> "plugin:${plugin.pluginId}:${option.key}=${option.value}" } }
        .toTypedArray()
    pluginOrderConstraints = (pluginOrderConstraints ?: emptyArray()) + filteredPlugins.flatMap { plugin ->
        plugin.orderingRequirements.map { order ->
            when (order.relation) {
                CompilerPluginPartialOrderRelation.BEFORE -> "${plugin.pluginId}>${order.otherPluginId}"
                CompilerPluginPartialOrderRelation.AFTER -> "${order.otherPluginId}>${plugin.pluginId}"
            }
        }
    }
        .toSet() // avoid duplicates
        .toTypedArray()
}

private fun validatePluginsConfiguration(plugins: List<CompilerPlugin>) {
    for (plugin in plugins) {
        // Empty plugin id
        if (plugin.pluginId.isBlank()) {
            throw IllegalStateException("Invalid compiler plugin configuration: plugin id is empty.")
        }
        if (plugin.orderingRequirements.any { it.otherPluginId.isBlank() }) throw IllegalStateException("Invalid compiler plugin configuration: plugin id is empty in the ordering requirements for plugin '${plugin.pluginId}'.")

        // Empty classpath
        if (plugin.classpath.isEmpty()) {
            throw IllegalStateException(
                "Invalid compiler plugin configuration: plugin '${plugin.pluginId}' has empty classpath."
            )
        }
    }
}

internal const val RAW_PLUGIN_ID = "___RAW_PLUGINS_APPLIED___"

internal fun applyCompilerPlugins(
    currentValue: List<CompilerPlugin>?,
    compilerArgs: CommonCompilerArguments,
): List<CompilerPlugin> {
    val normalizedCurrentValue = currentValue ?: emptyList()
    val rawValue = if (compilerArgs.pluginClasspaths == null && compilerArgs.pluginConfigurations == null) {
        emptyList()
    } else {
        listOf(
            CompilerPlugin(
                pluginId = RAW_PLUGIN_ID,
                classpath = emptyList(),
                rawArguments = emptyList(),
                orderingRequirements = emptySet(),
            )
        )
    }
    return normalizedCurrentValue + rawValue
}