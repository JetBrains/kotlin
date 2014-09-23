/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.cli.jvm

import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.jet.cli.common.arguments.CommonCompilerArguments
import kotlin.platform.*
import java.util.jar.JarFile
import java.util.jar.Attributes
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import java.util.regex.Pattern
import org.jetbrains.jet.utils.valuesToMap
import org.jetbrains.jet.config.CompilerConfiguration
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.compiler.plugin.CliOption

public object PluginCliParser {

    public val PLUGIN_ARGUMENT_PREFIX: String = "plugin:"

    private fun getCommandLineProcessors(arguments: CommonCompilerArguments): Collection<CommandLineProcessor> {
        return arguments.pluginClasspaths!!.map {
            loadCommandLineProcessor(JarFile(it).getManifest()?.getAttributes("org.jetbrains.kotlin.compiler.plugin"))
        }.filterNotNull()
    }

    private fun loadCommandLineProcessor(attributes: Attributes?): CommandLineProcessor? {
        if (attributes == null) return null

        val processorClassName = attributes.getValue("CommandLineProcessor")
        if (processorClassName == null) return null

        try {
            val processorClass = Class.forName(processorClassName)
            return processorClass.newInstance() as CommandLineProcessor
        }
        catch (e: Throwable) {
            throw CliOptionProcessingException("Loading plugin component failed: $processorClassName", e)
        }
    }

    [platformStatic]
    fun processPluginOptions(arguments: CommonCompilerArguments, configuration: CompilerConfiguration) {
        val optionValuesByPlugin = arguments.pluginOptions!!.map { parsePluginOption(it) }.groupBy {
            if (it == null) throw CliOptionProcessingException("Wrong plugin option format: $it, should be ${CommonCompilerArguments.PLUGIN_OPTION_FORMAT}")
            it.optionName
        }

        val processors = getCommandLineProcessors(arguments)
        for (processor in processors) {
            val declaredOptions = processor.pluginOptions.valuesToMap { it.name }
            val optionsToValues = MultiMap<CliOption, PluginOptionValue>()

            for (optionValue in optionValuesByPlugin[processor.pluginId].orEmpty()) {
                val option = declaredOptions[optionValue!!.optionName]
                if (option == null) {
                    throw CliOptionProcessingException("Unsupported plugin option: $optionValue")
                }
                optionsToValues.putValue(option, optionValue)
            }

            for (option in processor.pluginOptions) {
                val values = optionsToValues[option]
                if (option.required && values.isEmpty()) {
                    throw CliOptionProcessingException("Required plugin option not present: ${processor.pluginId}:${option.name}")
                }
                if (!option.allowMultipleOccurrences && values.size() > 1) {
                    throw CliOptionProcessingException("Multiple values not allowed for plugin option ${processor.pluginId}:${option.name}")
                }

                for (value in values) {
                    processor.processOption(option, value.value, configuration)
                }
            }
        }
    }

    private class PluginOptionValue(
            val pluginId: String,
            val optionName: String,
            val value: String
    ) {
        override fun toString() = "$pluginId:$optionName=$value"
    }

    private fun parsePluginOption(argumentValue: String): PluginOptionValue? {
        val pattern = Pattern.compile("""^([^:]*):([^=]*)=(.*)$""")
        val matcher = pattern.matcher(argumentValue)
        if (matcher.matches()) {
            return PluginOptionValue(matcher.group(1)!!, matcher.group(2)!!, matcher.group(3)!!)
        }

        return null
    }
}