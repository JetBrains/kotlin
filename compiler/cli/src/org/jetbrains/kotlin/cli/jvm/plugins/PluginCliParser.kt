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
import org.jetbrains.kotlin.cli.CliDiagnostics.COMPILER_ARGUMENTS_ERROR
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.plugins.*
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.cli.reportException
import org.jetbrains.kotlin.cli.reportInfo
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.util.ServiceLoaderLite
import org.jetbrains.kotlin.utils.graalvm.BundledCompilerPlugins
import org.jetbrains.kotlin.utils.graalvm.BundledPluginInfo
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
    ): ExitCode = loadPluginsSafe(configuration) {
        // Parse order constraints before creating class loaders and loading services.
        val orderConstraints = pluginOrderConstraints.map { rawConstraint ->
            extractPluginOrderConstraint(rawConstraint)
                ?: throw PluginProcessingException("Could not parse plugin order constraint: $rawConstraint")
        }

        loadPluginsLegacyStyle(pluginClasspaths, orderConstraints, pluginOptions, configuration, parentDisposable)
        loadPluginsModernStyle(pluginConfigurations, orderConstraints, configuration, parentDisposable)
    }

    /**
     * Loads native image-bundled compiler plugins from the given configurations/classpaths
     */
    internal fun loadBundledCompilerPlugins(
        pluginConfigurations: List<String>,
        pluginOptions: List<String>,
        pluginClasspaths: List<String>,
        pluginOrderConstraints: List<String>,
        configuration: CompilerConfiguration,
    ): ExitCode = loadPluginsSafe(configuration) {
        val [requestedBundledPlugins, nonBundledPlugins] = findRequestedBundledPlugins(
            pluginConfigurations,
            pluginClasspaths,
            pluginOptions
        )

        if (nonBundledPlugins.isNotEmpty()) {
            configuration.report(
                COMPILER_ARGUMENTS_ERROR,
                "Compiler plugin(s) cannot be loaded by the native-image compiler: ${nonBundledPlugins.joinToString("\n")}. " +
                        "Only bundled plugins are supported. " +
                        "Bundled plugins: ${BundledCompilerPlugins.pluginInfos.joinToString { it.pluginId }}."
            )
        }

        val pluginsById = requestedBundledPlugins.associateBy { it.id }
        val orderConstraints = pluginOrderConstraints.map { rawConstraint ->
            extractPluginOrderConstraint(rawConstraint)
                ?: throw PluginProcessingException("Could not parse plugin order constraint: $rawConstraint")
        }
        val dependenciesById = orderConstraints
            .filter { it.before in pluginsById && it.after in pluginsById }
            .groupBy(keySelector = { it.after }, valueTransform = { it.before })

        val orderedPluginIds = topologicalSort(
            nodes = pluginsById.keys,
            reportCycle = {
                throw PluginProcessingException(
                    "Compiler plugin '${it}' is part of an constraint cycle: ${orderConstraints.joinToString(", ")}"
                )
            },
            dependencies = { dependenciesById[this].orEmpty() }
        ).asReversed()

        for (plugin in orderedPluginIds) {
            loadBundledPlugin(pluginsById[plugin]!!.info, pluginsById[plugin]!!.options, configuration)
        }
    }

    private fun loadPluginsSafe(configuration: CompilerConfiguration, action: () -> Unit): ExitCode {
        try {
            action()
            return ExitCode.OK
        } catch (e: PluginProcessingException) {
            configuration.report(COMPILER_ARGUMENTS_ERROR, e.message!!)
        } catch (e: PluginCliOptionProcessingException) {
            val message = e.message + "\n\n" + cliPluginUsageString(e.pluginId, e.options)
            configuration.report(COMPILER_ARGUMENTS_ERROR, message)
        } catch (e: CliOptionProcessingException) {
            configuration.report(COMPILER_ARGUMENTS_ERROR, e.message!!)
        } catch (t: Throwable) {
            configuration.reportException(t)
        }
        return ExitCode.INTERNAL_ERROR
    }

    class RegisteredPluginInfo(
        val compilerPluginRegistrar: CompilerPluginRegistrar?,
        val commandLineProcessor: CommandLineProcessor?,
        val pluginOptions: List<CliOptionValue>,
    )

    @Suppress("DEPRECATION_ERROR")
    private fun loadRegisteredPluginsInfo(
        rawPluginConfigurations: Iterable<String>,
        orderConstraints: List<PluginOrderConstraint>,
        parentDisposable: Disposable,
    ): List<RegisteredPluginInfo> {
        val pluginConfigurations = extractPluginClasspathAndOptions(rawPluginConfigurations)

        val pluginInfos = pluginConfigurations.map { pluginConfiguration ->
            val classLoader = createClassLoader(pluginConfiguration.classpath, parentDisposable)
            val compilerPluginRegistrars = ServiceLoaderLite.loadImplementations(CompilerPluginRegistrar::class.java, classLoader)

            fun multiplePluginsErrorMessage(pluginObjects: List<Any>): String {
                return buildString {
                    append("Multiple plugins found in given classpath: ")
                    val extensionNames = pluginObjects.mapNotNull { it::class.qualifiedName }
                    appendLine(extensionNames.joinToString(", "))
                    append("  Plugin configuration is: ${pluginConfiguration.rawArgument}")
                }
            }

            when (compilerPluginRegistrars.size) {
                0 -> throw PluginProcessingException("No plugins found in given classpath: ${pluginConfiguration.classpath.joinToString(",")}")
                1 -> {}
                else -> throw PluginProcessingException(multiplePluginsErrorMessage(compilerPluginRegistrars))
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
        orderConstraints: List<PluginOrderConstraint>,
        configuration: CompilerConfiguration,
        parentDisposable: Disposable,
    ) {
        val pluginInfos = loadRegisteredPluginsInfo(rawPluginConfigurations, orderConstraints, parentDisposable)
        for (pluginInfo in pluginInfos) {
            pluginInfo.compilerPluginRegistrar?.let { configuration.add(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, it) }

            if (pluginInfo.pluginOptions.isEmpty()) continue
            val commandLineProcessor = pluginInfo.commandLineProcessor ?: throw RuntimeException() // TODO: proper exception
            processCompilerPluginOptions(commandLineProcessor, pluginInfo.pluginOptions, configuration)
        }
    }

    private class RequestedBundledPluginInfo(
        val id: String,
        val info: BundledPluginInfo,
        val options: List<CliOptionValue>,
    )

    /**
     * Parses the [pluginConfigurations] and [pluginClasspaths] and returns a list of
     * bundled plugins that are requested in them correspondingly. As a second parameter
     * returns a list of non-bundled plugins that were requested to register.
     */
    private fun findRequestedBundledPlugins(
        pluginConfigurations: List<String>,
        pluginClasspaths: List<String>,
        pluginOptions: List<String>,
    ): Pair<List<RequestedBundledPluginInfo>, List<String>> {
        val bundledPlugins = mutableListOf<RequestedBundledPluginInfo>()
        val nonBundledPlugins = mutableListOf<String>()

        fun register(info: BundledPluginInfo?, configuration: String, options: List<CliOptionValue>) {
            if (info == null) {
                nonBundledPlugins += configuration
                return
            }
            bundledPlugins += RequestedBundledPluginInfo(info.pluginId, info, options)
        }

        for (pluginConfiguration in pluginConfigurations) {
            val [_, classpath, options] = extractPluginClasspathAndOptions(pluginConfiguration)
            val info = classpath.firstNotNullOfOrNull { BundledCompilerPlugins.lookupByClasspathEntry(it) }
            register(info, pluginConfiguration, options)
        }

        for (classpath in pluginClasspaths) {
            val info = BundledCompilerPlugins.lookupByClasspathEntry(classpath)
            val options = pluginOptions.mapNotNull { parseLegacyPluginOption(it) }.filter { it.pluginId == info?.pluginId }
            register(info, classpath, options)
        }

        return bundledPlugins to nonBundledPlugins
    }

    /**
     * Loads the bundled plugin's registrar from the current classloader and
     * provides the user-provided options if there are any
     */
    private fun loadBundledPlugin(
        info: BundledPluginInfo,
        options: List<CliOptionValue>,
        configuration: CompilerConfiguration,
    ) {
        val classLoader = PluginCliParser::class.java.classLoader
        val registrar = try {
            classLoader.loadClass(info.pluginRegistrarFqName).getDeclaredConstructor().newInstance() as CompilerPluginRegistrar
        } catch (e: Throwable) {
            throw PluginProcessingException("Could not create '${info.pluginRegistrarFqName}' plugin registrar.", e)
        }

        configuration.reportInfo("Loading bundled compiler plugin '${info.pluginId}'.")
        configuration.add(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, registrar)

        if (options.isEmpty()) return
        val commandLineProcessor = info.commandLineProcessorFqName?.let {
            classLoader.loadClass(it).getDeclaredConstructor().newInstance() as? CommandLineProcessor
                ?: throw IllegalStateException("Could not instantiate $it")
        } ?: return
        processCompilerPluginOptions(commandLineProcessor, options, configuration)
    }

    @JvmStatic
    @Suppress("DEPRECATION_ERROR")
    private fun loadPluginsLegacyStyle(
        pluginClasspaths: Iterable<String>?,
        orderConstraints: List<PluginOrderConstraint>,
        pluginOptions: Iterable<String>?,
        configuration: CompilerConfiguration,
        parentDisposable: Disposable,
    ) {
        val classLoader = createClassLoader(pluginClasspaths ?: emptyList(), parentDisposable)
        val compilerPluginRegistrars = ServiceLoaderLite.loadImplementations(CompilerPluginRegistrar::class.java, classLoader)

        val registrarsById = compilerPluginRegistrars
            .filter {
                try {
                    it.pluginId.isNotEmpty()
                } catch (e: LinkageError) {
                    throw PluginProcessingError(
                        message = "Plugin ${it::class.qualifiedName} is incompatible with the current version of the compiler.",
                        cause = e
                    )
                }
            }
            .associateBy { it.pluginId }

        val dependenciesById = orderConstraints
            .filter { it.before in registrarsById && it.after in registrarsById }
            .groupBy(keySelector = { it.after }, valueTransform = { registrarsById.getValue(it.before) })

        val topologicalSort = topologicalSort(
            compilerPluginRegistrars,
            reportCycle = {
                val pluginId = it.pluginId
                throw PluginProcessingException(
                    "Compiler plugin '$pluginId' is part of an constraint cycle: ${orderConstraints.joinToString(", ")}"
                )
            },
            dependencies = { dependenciesById[pluginId].orEmpty() }
        )

        configuration.addAll(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, topologicalSort.asReversed())

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

    class PluginProcessingError(message: String, cause: Throwable?) : Error(message, cause)
}
