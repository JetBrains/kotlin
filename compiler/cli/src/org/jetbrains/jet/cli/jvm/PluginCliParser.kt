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
import java.util.ServiceLoader
import java.io.IOException
import java.util.Enumeration


public object PluginCliParser {

    public val PLUGIN_ARGUMENT_PREFIX: String = "plugin:"

    [platformStatic]
    fun loadPlugins(arguments: CommonCompilerArguments, configuration: CompilerConfiguration) {
        val classLoader = PluginURLClassLoader(
                arguments.pluginClasspaths
                        ?.map { File(it).toURI().toURL() }
                        ?.copyToArray()
                        ?: array<URL>(),
                javaClass.getClassLoader()
        )

        configuration.addAll(
                ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS,
                ServiceLoader.load(javaClass<ComponentRegistrar>(), classLoader).toList()
        )

        processPluginOptions(arguments, configuration, classLoader)
    }

    private fun processPluginOptions(
            arguments: CommonCompilerArguments,
            configuration: CompilerConfiguration,
            classLoader: ClassLoader
    ) {
        val optionValuesByPlugin = arguments.pluginOptions?.map { parsePluginOption(it) }?.groupBy {
            if (it == null) throw CliOptionProcessingException("Wrong plugin option format: $it, should be ${CommonCompilerArguments.PLUGIN_OPTION_FORMAT}")
            it.pluginId
        } ?: mapOf()

        val commandLineProcessors = ServiceLoader.load(javaClass<CommandLineProcessor>(), classLoader).toList()

        for (processor in commandLineProcessors) {
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

private class PluginURLClassLoader(urls: Array<URL>, parent: ClassLoader) : ClassLoader(Thread.currentThread().getContextClassLoader()) {
    private val childClassLoader: SelfThenParentURLClassLoader = SelfThenParentURLClassLoader(urls, parent)

    override synchronized fun loadClass(name: String, resolve: Boolean): Class<*> {
        return try {
            childClassLoader.findClass(name)
        }
        catch (e: ClassNotFoundException) {
            super.loadClass(name, resolve)
        }
    }

    override fun getResources(name: String) = childClassLoader.getResources(name)

    private class SelfThenParentURLClassLoader(urls: Array<URL>, val onFail: ClassLoader) : URLClassLoader(urls, null) {

        public override fun findClass(name: String): Class<*> {
            return try {
                super.findClass(name)
            } catch (e: ClassNotFoundException) {
                onFail.loadClass(name)
            }

        }
    }
}