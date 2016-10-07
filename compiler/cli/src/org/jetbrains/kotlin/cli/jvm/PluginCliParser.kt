/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.jvm

import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.*


object PluginCliParser {

    @JvmStatic
    fun loadPlugins(arguments: CommonCompilerArguments, configuration: CompilerConfiguration) {
        val classLoader = PluginURLClassLoader(
                arguments.pluginClasspaths
                        ?.map { File(it).toURI().toURL() }
                        ?.toTypedArray()
                        ?: arrayOf<URL>(),
                javaClass.classLoader
        )

        val componentRegistrars = ServiceLoader.load(ComponentRegistrar::class.java, classLoader).toMutableList()
        componentRegistrars.addAll(BundledCompilerPlugins.componentRegistrars)
        configuration.addAll(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS, componentRegistrars)

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

        val commandLineProcessors = ServiceLoader.load(CommandLineProcessor::class.java, classLoader).toMutableList()
        commandLineProcessors.addAll(BundledCompilerPlugins.commandLineProcessors)

        for (processor in commandLineProcessors) {
            val declaredOptions = processor.pluginOptions.associateBy { it.name }
            val optionsToValues = MultiMap<CliOption, CliOptionValue>()

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
                    throw PluginCliOptionProcessingException(
                            processor.pluginId,
                            processor.pluginOptions,
                            "Required plugin option not present: ${processor.pluginId}:${option.name}")
                }
                if (!option.allowMultipleOccurrences && values.size > 1) {
                    throw PluginCliOptionProcessingException(
                            processor.pluginId,
                            processor.pluginOptions,
                            "Multiple values are not allowed for plugin option ${processor.pluginId}:${option.name}")
                }

                for (value in values) {
                    processor.processOption(option, value.value, configuration)
                }
            }
        }
    }
}

private class PluginURLClassLoader(urls: Array<URL>, parent: ClassLoader) : ClassLoader(Thread.currentThread().contextClassLoader) {
    private val childClassLoader: SelfThenParentURLClassLoader = SelfThenParentURLClassLoader(urls, parent)

    @Synchronized
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
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
            val loaded = findLoadedClass(name)
            if (loaded != null) {
                return loaded
            }

            return try {
                super.findClass(name)
            }
            catch (e: ClassNotFoundException) {
                onFail.loadClass(name)
            }

        }
    }
}
