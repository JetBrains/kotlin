/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.plugins

import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration

data class PluginClasspathAndOptions(
    val rawArgument: String,
    val classpath: List<String>,
    val options: List<CliOptionValue>
)

private const val regularDelimiter = ","
private const val classpathOptionsDelimiter = "="

fun extractPluginClasspathAndOptions(pluginConfigurations: Iterable<String>): List<PluginClasspathAndOptions> {
    return pluginConfigurations.map { extractPluginClasspathAndOptions(it)}
}

fun extractPluginClasspathAndOptions(pluginConfiguration: String): PluginClasspathAndOptions {
    val rawClasspath = pluginConfiguration.substringBefore(classpathOptionsDelimiter)
    val rawOptions = pluginConfiguration.substringAfter(classpathOptionsDelimiter, missingDelimiterValue = "")
    val classPath = rawClasspath.split(regularDelimiter)
    val options = rawOptions.takeIf { it.isNotBlank() }
        ?.split(regularDelimiter)
        ?.mapNotNull { parseModernPluginOption(it) }
        ?: emptyList()
    return PluginClasspathAndOptions(pluginConfiguration, classPath, options)
}

fun processCompilerPluginsOptions(
    configuration: CompilerConfiguration,
    pluginOptions: Iterable<String>?,
    commandLineProcessors: List<CommandLineProcessor>
) {
    val optionValuesByPlugin = pluginOptions?.map(::parseLegacyPluginOption)?.groupBy {
        if (it == null) throw CliOptionProcessingException("Wrong plugin option format: $it, should be ${CommonCompilerArguments.PLUGIN_OPTION_FORMAT}")
        it.pluginId
    } ?: mapOf()

    for (processor in commandLineProcessors) {
        @Suppress("UNCHECKED_CAST")
        processCompilerPluginOptions(processor, optionValuesByPlugin[processor.pluginId].orEmpty() as List<CliOptionValue>, configuration)
    }
}

fun processCompilerPluginOptions(
    processor: CommandLineProcessor,
    pluginOptions: List<CliOptionValue>,
    configuration: CompilerConfiguration
) {
    val declaredOptions = processor.pluginOptions.associateBy { it.optionName }
    val optionsToValues = MultiMap<AbstractCliOption, CliOptionValue>()

    for (optionValue in pluginOptions) {
        val option = declaredOptions[optionValue.optionName]
            ?: throw CliOptionProcessingException("Unsupported plugin option: $optionValue")
        optionsToValues.putValue(option, optionValue)
    }

    for (option in processor.pluginOptions) {
        val values = optionsToValues[option]
        if (option.required && values.isEmpty()) {
            throw PluginCliOptionProcessingException(
                processor.pluginId,
                processor.pluginOptions,
                "Required plugin option not present: ${processor.pluginId}:${option.optionName}"
            )
        }
        if (!option.allowMultipleOccurrences && values.size > 1) {
            throw PluginCliOptionProcessingException(
                processor.pluginId,
                processor.pluginOptions,
                "Multiple values are not allowed for plugin option ${processor.pluginId}:${option.optionName}"
            )
        }

        for (value in values) {
            processor.processOption(option, value.value, configuration)
        }
    }
}
