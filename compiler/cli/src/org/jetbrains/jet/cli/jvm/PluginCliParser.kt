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
import org.jetbrains.jet.cli.common.messages.MessageCollector
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation
import java.net.URLClassLoader
import java.net.URL
import java.io.File
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar

public object PluginCliParser {

    public val PLUGIN_ARGUMENT_PREFIX: String = "plugin:"

    private class Context(
            val arguments: CommonCompilerArguments,
            val configuration: CompilerConfiguration,
            val classLoader: ClassLoader,
            val pluginManifestAttributes: List<Attributes>
    )

    [platformStatic]
    fun loadPlugins(arguments: CommonCompilerArguments, configuration: CompilerConfiguration) {
        val classLoader = URLClassLoader(arguments.pluginClasspaths?.map {File(it).toURI().toURL()}?.copyToArray() ?: array<URL>(), javaClass.getClassLoader())
        val pluginManifestAttributes = arguments.pluginClasspaths?.map {
            JarFile(it).getManifest()?.getAttributes("org.jetbrains.kotlin.compiler.plugin")
        }?.filterNotNull() ?: listOf()
        val context = Context(arguments, configuration, classLoader, pluginManifestAttributes)
        context.loadComponentRegistrars()
        context.processPluginOptions()
    }

    private fun Context.loadComponentRegistrars() {
        for (attributes in pluginManifestAttributes) {
            val registrar = loadComponent<ComponentRegistrar>(attributes, "ComponentRegistrar")
            if (registrar == null) continue

            configuration.add(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS, registrar)
        }
    }

    private fun Context.loadComponent<T: Any>(attributes: Attributes, componentName: String): T? {
        val processorClassName = attributes.getValue(componentName)
        if (processorClassName == null) return null

        try {
            val processorClass = Class.forName(processorClassName, true, classLoader)
            [suppress("UNCHECKED_CAST")]
            return processorClass.newInstance() as T
        }
        catch (e: Throwable) {
            throw CliOptionProcessingException("Loading plugin component failed: $componentName=$processorClassName ($e)", e)
        }
    }

    private fun Context.processPluginOptions() {
        val optionValuesByPlugin = arguments.pluginOptions?.map { parsePluginOption(it) }?.groupBy {
            if (it == null) throw CliOptionProcessingException("Wrong plugin option format: $it, should be ${CommonCompilerArguments.PLUGIN_OPTION_FORMAT}")
            it.pluginId
        } ?: mapOf()

        val processors = pluginManifestAttributes.map { loadComponent<CommandLineProcessor>(it, "CommandLineProcessor") }.filterNotNull()
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
        val pattern = Pattern.compile("""^plugin:([^:]*):([^=]*)=(.*)$""")
        val matcher = pattern.matcher(argumentValue)
        if (matcher.matches()) {
            return PluginOptionValue(matcher.group(1)!!, matcher.group(2)!!, matcher.group(3)!!)
        }

        return null
    }
}