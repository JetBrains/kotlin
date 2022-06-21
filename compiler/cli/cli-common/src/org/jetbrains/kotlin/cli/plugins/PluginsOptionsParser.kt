/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.plugins

import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration

fun processCompilerPluginsOptions(
    configuration: CompilerConfiguration,
    pluginOptions: Iterable<String>?,
    commandLineProcessors: List<CommandLineProcessor>
) {
    val optionValuesByPlugin = pluginOptions?.map(::parsePluginOption)?.groupBy {
        if (it == null) throw CliOptionProcessingException("Wrong plugin option format: $it, should be ${CommonCompilerArguments.PLUGIN_OPTION_FORMAT}")
        it.pluginId
    } ?: mapOf()

    for (processor in commandLineProcessors) {
        val declaredOptions = processor.pluginOptions.associateBy { it.optionName }
        val optionsToValues = MultiMap<AbstractCliOption, CliOptionValue>()

        for (optionValue in optionValuesByPlugin[processor.pluginId].orEmpty()) {
            val option = declaredOptions[optionValue!!.optionName]
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
}
