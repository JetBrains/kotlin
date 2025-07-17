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

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil
import org.jetbrains.kotlin.cli.plugins.extractPluginClasspathAndOptions
import org.jetbrains.kotlin.cli.plugins.extractPluginOrderConstraint
import org.jetbrains.kotlin.cli.plugins.processCompilerPluginOptions
import org.jetbrains.kotlin.cli.plugins.processCompilerPluginsOptions
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.util.ServiceLoaderLite
import org.jetbrains.kotlin.utils.topologicalSort
import java.io.File
import java.lang.ref.WeakReference
import java.net.URLClassLoader

object PluginCliParser {
    @JvmStatic
    @Deprecated(
        "Use loadPluginsSafe with order constraints instead",
        ReplaceWith("loadPluginsSafe(pluginClasspaths, pluginOptions, pluginConfigurations, emptyList(), configuration, parentDisposable)")
    )
    fun loadPluginsSafe(
        pluginClasspaths: Array<String>?,
        pluginOptions: Array<String>?,
        pluginConfigurations: Array<String>?,
        configuration: CompilerConfiguration,
        parentDisposable: Disposable,
    ): ExitCode {
        return loadPluginsSafe(
            pluginClasspaths?.asList().orEmpty(),
            pluginOptions?.asList().orEmpty(),
            pluginConfigurations?.asList().orEmpty(),
            emptyList(),
            configuration,
            parentDisposable,
        )
    }

    @JvmStatic
    fun loadPluginsSafe(
        pluginClasspaths: Array<String>?,
        pluginOptions: Array<String>?,
        pluginConfigurations: Array<String>?,
        pluginOrderConstraints: Array<String>?,
        configuration: CompilerConfiguration,
        parentDisposable: Disposable,
    ): ExitCode {
        return loadPluginsSafe(
            pluginClasspaths?.asList().orEmpty(),
            pluginOptions?.asList().orEmpty(),
            pluginConfigurations?.asList().orEmpty(),
            pluginOrderConstraints?.asList().orEmpty(),
            configuration,
            parentDisposable,
        )
    }

    @JvmStatic
    @Deprecated(
        "Use loadPluginsSafe with order constraints instead",
        ReplaceWith("loadPluginsSafe(pluginClasspaths, pluginOptions, pluginConfigurations, emptyList(), configuration, parentDisposable)")
    )
    fun loadPluginsSafe(
        pluginClasspaths: Collection<String>,
        pluginOptions: Collection<String>,
        pluginConfigurations: Collection<String>,
        configuration: CompilerConfiguration,
        parentDisposable: Disposable,
    ): ExitCode {
        return loadPluginsSafe(pluginClasspaths, pluginOptions, pluginConfigurations, emptyList(), configuration, parentDisposable)
    }

    @JvmStatic
    fun loadPluginsSafe(
        pluginClasspaths: Collection<String>,
        pluginOptions: Collection<String>,
        pluginConfigurations: Collection<String>,
        pluginOrderConstraints: Collection<String>,
        configuration: CompilerConfiguration,
        parentDisposable: Disposable,
    ): ExitCode {
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        try {
            loadPluginsLegacyStyle(pluginClasspaths, pluginOptions, configuration, parentDisposable)
            loadPluginsModernStyle(pluginConfigurations, pluginOrderConstraints, configuration, parentDisposable)
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
        @Suppress("DEPRECATION_ERROR") val componentRegistrar: ComponentRegistrar?,
        val compilerPluginRegistrar: CompilerPluginRegistrar?,
        val commandLineProcessor: CommandLineProcessor?,
        val pluginOptions: List<CliOptionValue>,
    )

    @Suppress("DEPRECATION_ERROR")
    private fun loadRegisteredPluginsInfo(
        rawPluginConfigurations: Iterable<String>,
        rawPluginOrderConstraints: Iterable<String>,
        parentDisposable: Disposable,
    ): List<RegisteredPluginInfo> {
        val pluginConfigurations = extractPluginClasspathAndOptions(rawPluginConfigurations)

        // Parse order constraints before creating class loaders and loading services.
        val orderConstraints = rawPluginOrderConstraints.map { rawConstraint ->
            extractPluginOrderConstraint(rawConstraint)
                ?: throw PluginProcessingException("Could not parse plugin order constraint: $rawConstraint")
        }

        val pluginInfos = pluginConfigurations.map { pluginConfiguration ->
            val classLoader = createClassLoader(pluginConfiguration.classpath, parentDisposable)
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

            val commandLineProcessors = ServiceLoaderLite.loadImplementations(CommandLineProcessor::class.java, classLoader)
            if (commandLineProcessors.size > 1) {
                throw PluginProcessingException(multiplePluginsErrorMessage(commandLineProcessors))
            }

            val commandLineProcessor = commandLineProcessors.firstOrNull()
            val compilerPluginRegistrar = compilerPluginRegistrars.firstOrNull()
            if (commandLineProcessor != null && !compilerPluginRegistrar?.pluginId.isNullOrEmpty() &&
                commandLineProcessor.pluginId != compilerPluginRegistrar.pluginId
            ) {
                throw PluginProcessingException(
                    "Mismatched 'pluginId's between registrar (${compilerPluginRegistrar.pluginId}) and processor (${commandLineProcessor.pluginId})}"
                )
            }

            RegisteredPluginInfo(
                componentRegistrars.firstOrNull(),
                compilerPluginRegistrar,
                commandLineProcessor,
                pluginConfiguration.options
            )
        }

        val registrarsById = pluginInfos
            .filter { !it.compilerPluginRegistrar?.pluginId.isNullOrEmpty() }
            .associateBy { it.compilerPluginRegistrar!!.pluginId }

        val dependenciesById = orderConstraints
            .filter { it.before in registrarsById && it.after in registrarsById }
            .groupBy(keySelector = { it.after }, valueTransform = { registrarsById.getValue(it.before) })

        val topologicalSort = topologicalSort(
            pluginInfos,
            reportCycle = {
                val pluginId = it.compilerPluginRegistrar?.pluginId
                throw PluginProcessingException(
                    "Compiler plugin '$pluginId' is part of an constraint cycle: ${orderConstraints.joinToString(", ")}"
                )
            },
            dependencies = { dependenciesById[compilerPluginRegistrar?.pluginId].orEmpty() }
        )

        return topologicalSort.asReversed()
    }

    private fun loadPluginsModernStyle(
        rawPluginConfigurations: Iterable<String>,
        rawPluginOrderConstraints: Iterable<String>,
        configuration: CompilerConfiguration,
        parentDisposable: Disposable,
    ) {
        val pluginInfos = loadRegisteredPluginsInfo(rawPluginConfigurations, rawPluginOrderConstraints, parentDisposable)
        for (pluginInfo in pluginInfos) {
            pluginInfo.componentRegistrar?.let {
                @Suppress("DEPRECATION_ERROR")
                configuration.add(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS, it)
            }
            pluginInfo.compilerPluginRegistrar?.let { configuration.add(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, it) }

            if (pluginInfo.pluginOptions.isEmpty()) continue
            val commandLineProcessor = pluginInfo.commandLineProcessor ?: throw RuntimeException() // TODO: proper exception
            processCompilerPluginOptions(commandLineProcessor, pluginInfo.pluginOptions, configuration)
        }
    }

    @JvmStatic
    @Suppress("DEPRECATION_ERROR")
    private fun loadPluginsLegacyStyle(
        pluginClasspaths: Iterable<String>?,
        pluginOptions: Iterable<String>?,
        configuration: CompilerConfiguration,
        parentDisposable: Disposable,
    ) {
        val classLoader = createClassLoader(pluginClasspaths ?: emptyList(), parentDisposable)
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

    private fun createClassLoader(classpath: Iterable<String>, parentDisposable: Disposable): URLClassLoader {
        val classLoader = URLClassLoader(classpath.map { File(it).toURI().toURL() }.toTypedArray(), this::class.java.classLoader)
        Disposer.register(parentDisposable, UrlClassLoaderDisposable(classLoader))
        return classLoader
    }

    // Disposer uses the identity of disposable to deduplicate registered disposables.
    // We should create a new instance every time to avoid unregistering a previous disposable.
    private class UrlClassLoaderDisposable(classLoader: URLClassLoader) : Disposable {
        // Allow the class loader to be garbage collected early if needed.
        private val classLoaderRef: WeakReference<URLClassLoader> = WeakReference(classLoader)

        override fun dispose() {
            val classLoader = classLoaderRef.get()
            if (classLoader != null) {
                classLoader.close()
                classLoaderRef.clear()
            }
        }
    }
}
