/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import org.jetbrains.kotlin.backend.jvm.jvmPhases
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.LOGGING
import org.jetbrains.kotlin.cli.jvm.*
import org.jetbrains.kotlin.cli.jvm.compiler.applyModuleProperties
import org.jetbrains.kotlin.cli.jvm.compiler.configureSourceRoots
import org.jetbrains.kotlin.cli.jvm.config.ClassicFrontendSpecificJvmConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.cli.pipeline.AbstractConfigurationPhase
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.ConfigurationUpdater
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.load.java.JavaClassesTracker
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import java.io.File

object JvmConfigurationPipelinePhase : AbstractConfigurationPhase<K2JVMCompilerArguments>(
    name = "JvmConfigurationPipelinePhase",
    postActions = setOf(CheckCompilationErrors.CheckMessageCollector),
    configurationUpdaters = listOf(JvmConfigurationUpdater)
) {
    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion {
        return MetadataVersion(*versionArray)
    }

    override fun provideCustomScriptingPluginOptions(arguments: K2JVMCompilerArguments): List<String> {
        return buildList {
            if (arguments.scriptTemplates?.isNotEmpty() == true) {
                add("plugin:kotlin.scripting:script-templates=${arguments.scriptTemplates!!.joinToString(",")}")
            }
            if (arguments.scriptResolverEnvironment?.isNotEmpty() == true) {
                add("plugin:kotlin.scripting:script-resolver-environment=${arguments.scriptResolverEnvironment!!.joinToString(",")}")
            }
        }
    }
}

object JvmConfigurationUpdater : ConfigurationUpdater<K2JVMCompilerArguments>() {
    override fun fillConfiguration(
        input: ArgumentsPipelineArtifact<K2JVMCompilerArguments>,
        configuration: CompilerConfiguration,
    ) {
        val (arguments, services, _, _, _) = input
        val messageCollector = configuration.messageCollector
        messageCollector.report(LOGGING, "Configuring the compilation environment")

        arguments.buildFile?.let { configuration.buildFile = File(it) }
        configuration.allowNoSourceFiles = arguments.allowNoSourceFiles
        configuration.setupJvmSpecificArguments(arguments)
        configuration.setupIncrementalCompilationServices(arguments, services)

        configuration.phaseConfig = createPhaseConfig(arguments).also {
            if (arguments.listPhases) it.list(jvmPhases)
        }
        if (!configuration.configureJdkHome(arguments)) return
        configuration.disableStandardScriptDefinition = arguments.disableStandardScript
        val moduleName = arguments.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME
        configuration.moduleName = moduleName
        configuration.configureJavaModulesContentRoots(arguments)
        configuration.configureStandardLibs(configuration.kotlinPaths, arguments)
        configuration.configureAdvancedJvmOptions(arguments)
        configuration.configureKlibPaths(arguments)
        if (arguments.expression == null) {
            configuration.setupModuleChunk(arguments)
        } else {
            configuration.configureContentRootsFromClassPath(arguments)
        }
        if (arguments.script || arguments.expression != null) {
            configuration.scriptMode = arguments.script
            configuration.freeArgsForScript += arguments.freeArgs
            configuration.expressionToEvaluate = arguments.expression
            configuration.defaultExtensionForScripts = arguments.defaultScriptExtension
        }
        // should be called after configuring jdk home from build file
        configuration.configureJdkClasspathRoots()
    }

    private fun CompilerConfiguration.setupIncrementalCompilationServices(arguments: K2JVMCompilerArguments, services: Services) {
        if (!incrementalCompilationIsEnabled(arguments)) return
        lookupTracker = services[LookupTracker::class.java]
        expectActualTracker = services[ExpectActualTracker::class.java]
        inlineConstTracker = services[InlineConstTracker::class.java]
        enumWhenTracker = services[EnumWhenTracker::class.java]
        importTracker = services[ImportTracker::class.java]
        incrementalCompilationComponents = services[IncrementalCompilationComponents::class.java]
        putIfNotNull(ClassicFrontendSpecificJvmConfigurationKeys.JAVA_CLASSES_TRACKER, services[JavaClassesTracker::class.java])
    }

    private fun CompilerConfiguration.setupModuleChunk(arguments: K2JVMCompilerArguments) {
        val buildFile = this.buildFile
        val moduleChunk = configureModuleChunk(arguments, buildFile)
        this.moduleChunk = moduleChunk
        if (moduleChunk.modules.size == 1) {
            applyModuleProperties(moduleChunk.modules.single(), buildFile)
        }
        configureSourceRoots(moduleChunk.modules, buildFile)
    }
}
