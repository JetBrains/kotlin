/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.arguments

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPluginOption
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPluginPartialOrder
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPluginPartialOrderRelation
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import java.nio.file.Path

internal fun CommonCompilerArguments.applyCompilerPlugins(plugins: List<CompilerPluginImpl>) {
    val filteredPlugins = plugins.filter { it.pluginId != RAW_PLUGIN_ID }
    validatePluginsConfiguration(filteredPlugins)
    pluginClasspaths += filteredPlugins.flatMap { it.classpath }.map { it.absolutePathStringOrThrow() }.toTypedArray()
    pluginOptions += filteredPlugins.flatMap { plugin -> plugin.rawArguments.map { option -> "plugin:${plugin.pluginId}:${option.key}=${option.value}" } }
        .toTypedArray()
    pluginOrderConstraints += filteredPlugins.flatMap { plugin ->
        plugin.orderingRequirements.map { order ->
            when (order.relation) {
                CompilerPluginPartialOrderRelationImpl.BEFORE -> "${plugin.pluginId}>${order.otherPluginId}"
                CompilerPluginPartialOrderRelationImpl.AFTER -> "${order.otherPluginId}>${plugin.pluginId}"
            }
        }
    }
        .toSet() // avoid duplicates
        .toTypedArray()
}

private fun validatePluginsConfiguration(plugins: List<CompilerPluginImpl>) {
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
    currentValue: List<CompilerPluginImpl>,
    compilerArgs: CommonCompilerArguments,
): List<CompilerPluginImpl> {
    val rawValue = if (compilerArgs.pluginClasspaths.isEmpty() && compilerArgs.pluginConfigurations.isEmpty()) {
        emptyList()
    } else {
        listOf(
            CompilerPluginImpl(
                pluginId = RAW_PLUGIN_ID,
                classpath = emptyList(),
                rawArguments = emptyList(),
                orderingRequirements = emptySet(),
            )
        )
    }
    return currentValue + rawValue
}

@Serializable
public class CompilerPluginOptionImpl(public val key: String, public val value: String) {
    internal fun toApi(): CompilerPluginOption = CompilerPluginOption(key, value)
}

internal fun CompilerPluginOption.toImpl(): CompilerPluginOptionImpl = CompilerPluginOptionImpl(key, value)

public enum class CompilerPluginPartialOrderRelationImpl {
    BEFORE,
    AFTER,
    ;

    internal fun toApi(): CompilerPluginPartialOrderRelation = CompilerPluginPartialOrderRelation.valueOf(name)
}

internal fun CompilerPluginPartialOrderRelation.toImpl(): CompilerPluginPartialOrderRelationImpl =
    CompilerPluginPartialOrderRelationImpl.valueOf(name)

@Serializable
public class CompilerPluginPartialOrderImpl(
    public val relation: CompilerPluginPartialOrderRelationImpl,
    public val otherPluginId: String,
) {
    internal fun toApi(): CompilerPluginPartialOrder = CompilerPluginPartialOrder(relation.toApi(), otherPluginId)
}

internal fun CompilerPluginPartialOrder.toImpl(): CompilerPluginPartialOrderImpl =
    CompilerPluginPartialOrderImpl(relation.toImpl(), otherPluginId)


@Serializable
public class CompilerPluginImpl(
    public val pluginId: String,
    public val classpath: List<Path>,
    public val rawArguments: List<CompilerPluginOptionImpl>,
    public val orderingRequirements: Set<CompilerPluginPartialOrderImpl>,
) {
    internal fun toApi(): CompilerPlugin = CompilerPlugin(
        pluginId,
        classpath,
        rawArguments.map(CompilerPluginOptionImpl::toApi),
        orderingRequirements.map(CompilerPluginPartialOrderImpl::toApi).toSet()
    )
}

internal fun CompilerPlugin.toImpl(): CompilerPluginImpl = CompilerPluginImpl(
    pluginId,
    classpath,
    rawArguments.map { it.toImpl() },
    orderingRequirements.map { it.toImpl() }.toSet()
)
