/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.cli.pipeline

import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.CLICompiler.Companion.SCRIPT_PLUGIN_COMMANDLINE_PROCESSOR_NAME
import org.jetbrains.kotlin.cli.common.CLICompiler.Companion.SCRIPT_PLUGIN_K2_REGISTRAR_NAME
import org.jetbrains.kotlin.cli.common.CLICompiler.Companion.SCRIPT_PLUGIN_REGISTRAR_NAME
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.LOGGING
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.cli.plugins.extractPluginClasspathAndOptions
import org.jetbrains.kotlin.cli.plugins.processCompilerPluginsOptions
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.phaser.Action
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

/**
 * Updates the compiler configuration using information from the compiler arguments
 */
abstract class ConfigurationUpdater<in A : CommonCompilerArguments> {
    abstract fun fillConfiguration(input: ArgumentsPipelineArtifact<A>, configuration: CompilerConfiguration)
}

// from CLICompiler
abstract class AbstractConfigurationPhase<A : CommonCompilerArguments>(
    name: String,
    preActions: Set<Action<ArgumentsPipelineArtifact<A>, PipelineContext>> = emptySet(),
    postActions: Set<Action<ConfigurationPipelineArtifact, PipelineContext>> = emptySet(),
    val configurationUpdaters: List<ConfigurationUpdater<A>>
) : PipelinePhase<ArgumentsPipelineArtifact<A>, ConfigurationPipelineArtifact>(name, preActions, postActions) {
    override fun executePhase(input: ArgumentsPipelineArtifact<A>): ConfigurationPipelineArtifact? {
        val configuration = CompilerConfiguration()
        configuration.setupCommonConfiguration(input)

        for (filler in configurationUpdaters) {
            filler.fillConfiguration(input, configuration)
        }

        return ConfigurationPipelineArtifact(configuration, input.diagnosticCollector, input.rootDisposable)
    }

    protected abstract fun createMetadataVersion(versionArray: IntArray): BinaryVersion
    protected open fun provideCustomScriptingPluginOptions(arguments: A): List<String> = emptyList()

    private fun CompilerConfiguration.setupCommonConfiguration(input: ArgumentsPipelineArtifact<A>) {
        val (arguments, _, _, messageCollector, performanceManager) = input
        this.messageCollector = messageCollector
        perfManager = performanceManager
        printVersion = arguments.version
        // TODO(KT-73711): move script-related configuration to JVM CLI
        scriptMode = arguments.script
        setupCommonArguments(arguments, ::createMetadataVersion)
        val paths = computeKotlinPaths(messageCollector, arguments)?.also {
            kotlinPaths = it
        }
        loadCompilerPlugins(paths, input, this)
    }

    private fun loadCompilerPlugins(
        paths: KotlinPaths?,
        input: ArgumentsPipelineArtifact<A>,
        configuration: CompilerConfiguration,
    ) {
        val arguments = input.arguments
        val pluginClasspaths = arguments.pluginClasspaths.orEmpty().toMutableList()
        val pluginOptions = arguments.pluginOptions.orEmpty().toMutableList()
        val pluginConfigurations = arguments.pluginConfigurations.orEmpty().toMutableList()
        val messageCollector = configuration.messageCollector

        if (!checkPluginsArguments(messageCollector, useK2 = true, pluginClasspaths, pluginOptions, pluginConfigurations)) {
            return
        }

        val scriptingPluginClasspath = mutableListOf<String>()
        val scriptingPluginOptions = mutableListOf<String>()

        if (!arguments.disableDefaultScriptingPlugin) {
            scriptingPluginOptions += provideCustomScriptingPluginOptions(arguments)
            val explicitScriptingPlugin =
                extractPluginClasspathAndOptions(pluginConfigurations).any { (_, classpath, _) ->
                    classpath.any { File(it).name.startsWith(PathUtil.KOTLIN_SCRIPTING_COMPILER_PLUGIN_NAME) }
                } || pluginClasspaths.any { File(it).name.startsWith(PathUtil.KOTLIN_SCRIPTING_COMPILER_PLUGIN_NAME) }
            val explicitOrLoadedScriptingPlugin = explicitScriptingPlugin ||
                    tryLoadScriptingPluginFromCurrentClassLoader(configuration, pluginOptions)
            if (!explicitOrLoadedScriptingPlugin) {
                val kotlinPaths = paths ?: PathUtil.kotlinPathsForCompiler
                val libPath = kotlinPaths.libPath.takeIf { it.exists() && it.isDirectory } ?: File(".")
                val (jars, missingJars) =
                    PathUtil.KOTLIN_SCRIPTING_PLUGIN_CLASSPATH_JARS.map { File(libPath, it) }.partition { it.exists() }
                if (missingJars.isEmpty()) {
                    scriptingPluginClasspath.addAll(0, jars.map { it.canonicalPath })
                } else {
                    messageCollector.report(
                        LOGGING,
                        "Scripting plugin will not be loaded: not all required jars are present in the classpath (missing files: $missingJars)"
                    )
                }
            }
        } else {
            scriptingPluginOptions.add("plugin:kotlin.scripting:disable=true")
        }

        pluginClasspaths.addAll(scriptingPluginClasspath)
        pluginOptions.addAll(scriptingPluginOptions)

        PluginCliParser.loadPluginsSafe(pluginClasspaths, pluginOptions, pluginConfigurations, configuration, input.rootDisposable)
    }

    private fun tryLoadScriptingPluginFromCurrentClassLoader(
        configuration: CompilerConfiguration,
        pluginOptions: List<String>,
    ): Boolean {
        return try {
            val pluginRegistrarClass = PluginCliParser::class.java.classLoader.loadClass(SCRIPT_PLUGIN_REGISTRAR_NAME)
            val pluginRegistrar = (pluginRegistrarClass.getDeclaredConstructor().newInstance() as? ComponentRegistrar)?.also {
                configuration.add(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS, it)
            }
            val pluginK2RegistrarClass = PluginCliParser::class.java.classLoader.loadClass(SCRIPT_PLUGIN_K2_REGISTRAR_NAME)
            val pluginK2Registrar = (pluginK2RegistrarClass.getDeclaredConstructor().newInstance() as? CompilerPluginRegistrar)?.also {
                configuration.add(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, it)
            }
            if (pluginRegistrar != null || pluginK2Registrar != null) {
                processScriptPluginCliOptions(pluginOptions, configuration)
                true
            } else false
        } catch (e: Throwable) {
            configuration.messageCollector.report(LOGGING, "Exception on loading scripting plugin: $e")
            false
        }
    }

    private fun processScriptPluginCliOptions(pluginOptions: List<String>, configuration: CompilerConfiguration) {
        val cmdlineProcessorClass =
            if (pluginOptions.isEmpty()) null
            else PluginCliParser::class.java.classLoader.loadClass(SCRIPT_PLUGIN_COMMANDLINE_PROCESSOR_NAME)!!
        val cmdlineProcessor = cmdlineProcessorClass?.getDeclaredConstructor()?.newInstance() as? CommandLineProcessor
        if (cmdlineProcessor != null) {
            processCompilerPluginsOptions(configuration, pluginOptions, listOf(cmdlineProcessor))
        }
    }
}
