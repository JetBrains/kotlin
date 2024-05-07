/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.jvm.plugins

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil
import org.jetbrains.kotlin.cli.plugins.extractPluginClasspathAndOptions
import org.jetbrains.kotlin.cli.plugins.processCompilerPluginOptions
import org.jetbrains.kotlin.cli.plugins.processCompilerPluginsOptions
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.util.ServiceLoaderLite
import java.io.File
import java.net.URLClassLoader

object PluginCliParser {
    @JvmStatic
    fun loadPluginsSafe(
        pluginClasspaths: Array<String>?,
        pluginOptions: Array<String>?,
        pluginConfigurations: Array<String>?,
        configuration: CompilerConfiguration
    ): ExitCode {
        return loadPluginsSafe(
            pluginClasspaths?.toList() ?: emptyList(),
            pluginOptions?.toList() ?: emptyList(),
            pluginConfigurations?.toList() ?: emptyList(),
            configuration
        )
    }

    @JvmStatic
    fun loadPluginsSafe(
        pluginClasspaths: Collection<String>,
        pluginOptions: Collection<String>,
        pluginConfigurations: Collection<String>,
        configuration: CompilerConfiguration
    ): ExitCode {
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        try {
            loadPluginsLegacyStyle(pluginClasspaths, pluginOptions, configuration)
            loadPluginsModernStyle(pluginConfigurations, configuration)
            return ExitCode.OK
        } catch (e: PluginProcessingException) {
            messageCollector.report(CompilerMessageSeverity.ERROR, e.message!!)
        } catch (e: PluginCliOptionProcessingException) {
            val message = e.message + "\n\n" + cliPluginUsageString(e.pluginId, e.options)
            messageCollector.report(CompilerMessageSeverity.ERROR, message)
        } catch (e: CliOptionProcessingException) {
            messageCollector.report(CompilerMessageSeverity.ERROR, e.message!!)
        } catch (t: Throwable) {
            MessageCollectorUtil.reportException(messageCollector, t)
        }
        return ExitCode.INTERNAL_ERROR
    }

    class RegisteredPluginInfo(
        @Suppress("DEPRECATION") val componentRegistrar: ComponentRegistrar?,
        val compilerPluginRegistrar: CompilerPluginRegistrar?,
        val commandLineProcessor: CommandLineProcessor?,
        val pluginOptions: List<CliOptionValue>
    )

    @Suppress("DEPRECATION")
    private fun loadRegisteredPluginsInfo(rawPluginConfigurations: Iterable<String>): List<RegisteredPluginInfo> {
        val pluginConfigurations = extractPluginClasspathAndOptions(rawPluginConfigurations)
        val pluginInfos = pluginConfigurations.map { pluginConfiguration ->
            val classLoader = createClassLoader(pluginConfiguration.classpath)
            val componentRegistrars = ServiceLoaderLite.loadImplementations(ComponentRegistrar::class.java, classLoader)
            val compilerPluginRegistrars = ServiceLoaderLite.loadImplementations(CompilerPluginRegistrar::class.java, classLoader)

            fun multiplePluginsErrorMessage(pluginObjects: List<Any>): String {
                return buildString {
                    append("Multiple plugins found in given classpath: ")
                    val extensionNames = pluginObjects.mapNotNull { it::class.qualifiedName }
                    appendLine(extensionNames.joinToString(", "))
                    append("  Plugin configuration is: ${pluginConfiguration.rawArgument}")
                }
            }

            when (componentRegistrars.size + compilerPluginRegistrars.size) {
                0 -> throw PluginProcessingException("No plugins found in given classpath: ${pluginConfiguration.classpath.joinToString(",")}")
                1 -> {}
                else -> throw PluginProcessingException(multiplePluginsErrorMessage(componentRegistrars + compilerPluginRegistrars))
            }

            val commandLineProcessor = ServiceLoaderLite.loadImplementations(CommandLineProcessor::class.java, classLoader)
            if (commandLineProcessor.size > 1) {
                throw PluginProcessingException(multiplePluginsErrorMessage(commandLineProcessor))
            }
            RegisteredPluginInfo(
                componentRegistrars.firstOrNull(),
                compilerPluginRegistrars.firstOrNull(),
                commandLineProcessor.firstOrNull(),
                pluginConfiguration.options
            )
        }
        return pluginInfos
    }

    private fun loadPluginsModernStyle(rawPluginConfigurations: Iterable<String>?, configuration: CompilerConfiguration) {
        if (rawPluginConfigurations == null) return
        val pluginInfos = loadRegisteredPluginsInfo(rawPluginConfigurations)
        for (pluginInfo in pluginInfos) {
            pluginInfo.componentRegistrar?.let {
                @Suppress("DEPRECATION")
                configuration.add(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS, it)
            }
            pluginInfo.compilerPluginRegistrar?.let { configuration.add(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, it) }

            if (pluginInfo.pluginOptions.isEmpty()) continue
            val commandLineProcessor = pluginInfo.commandLineProcessor ?: throw RuntimeException() // TODO: proper exception
            processCompilerPluginOptions(commandLineProcessor, pluginInfo.pluginOptions, configuration)
        }
    }

    @JvmStatic
    @Suppress("DEPRECATION")
    private fun loadPluginsLegacyStyle(
        pluginClasspaths: Iterable<String>?,
        pluginOptions: Iterable<String>?,
        configuration: CompilerConfiguration
    ) {
        val classLoader = createClassLoader(pluginClasspaths ?: emptyList())
        val componentRegistrars = ServiceLoaderLite.loadImplementations(ComponentRegistrar::class.java, classLoader)
        configuration.addAll(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS, componentRegistrars)

        val compilerPluginRegistrars = ServiceLoaderLite.loadImplementations(CompilerPluginRegistrar::class.java, classLoader)
        configuration.addAll(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, compilerPluginRegistrars)

        processPluginOptions(pluginOptions, configuration, classLoader)
    }

    private fun processPluginOptions(
        pluginOptions: Iterable<String>?,
        configuration: CompilerConfiguration,
        classLoader: URLClassLoader
    ) {
        // TODO issue a warning on using deprecated command line processors when all official plugin migrate to the newer convention
        val commandLineProcessors = ServiceLoaderLite.loadImplementations(CommandLineProcessor::class.java, classLoader)

        processCompilerPluginsOptions(configuration, pluginOptions, commandLineProcessors)
    }

    private fun createClassLoader(classpath: Iterable<String>): URLClassLoader {
        return URLClassLoader(classpath.map { File(it).toURI().toURL() }.toTypedArray(), this::class.java.classLoader)
    }
}
