/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.metadata

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.STRONG_WARNING
import org.jetbrains.kotlin.cli.common.perfManager
import org.jetbrains.kotlin.cli.jvm.config.K2MetadataConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.pipeline.AbstractConfigurationPhase
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.ConfigurationUpdater
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import java.io.File

object MetadataConfigurationPipelinePhase : AbstractConfigurationPhase<K2MetadataCompilerArguments>(
    name = "MetadataConfigurationPipelinePhase",
    postActions = setOf(CheckCompilationErrors.CheckMessageCollector),
    configurationUpdaters = listOf(MetadataConfigurationUpdater)
) {
    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion {
        return MetadataVersion(*versionArray)
    }
}

object MetadataConfigurationUpdater : ConfigurationUpdater<K2MetadataCompilerArguments>() {
    override fun fillConfiguration(
        input: ArgumentsPipelineArtifact<K2MetadataCompilerArguments>,
        configuration: CompilerConfiguration,
    ) {
        fillConfiguration(configuration, input.arguments)
    }

    fun fillConfiguration(configuration: CompilerConfiguration, arguments: K2MetadataCompilerArguments) {
        val collector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        val commonSources = arguments.commonSources?.toSet() ?: emptySet()
        val hmppCliModuleStructure = configuration.get(CommonConfigurationKeys.HMPP_MODULE_STRUCTURE)
        if (hmppCliModuleStructure != null) {
            collector.report(
                ERROR,
                "HMPP module structure should not be passed during metadata compilation. Please remove `-Xfragments` and related flags"
            )
            return
        }
        for (arg in arguments.freeArgs) {
            configuration.addKotlinSourceRoot(arg, isCommon = arg in commonSources, hmppModuleName = null)
        }
        if (arguments.classpath != null) {
            configuration.addJvmClasspathRoots(arguments.classpath!!.split(File.pathSeparatorChar).map(::File))
        }

        val moduleName = arguments.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME
        configuration.put(CommonConfigurationKeys.MODULE_NAME, moduleName)

        configuration.put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, arguments.allowKotlinPackage)
        configuration.put(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME, arguments.renderInternalDiagnosticNames)

        configuration.putIfNotNull(K2MetadataConfigurationKeys.FRIEND_PATHS, arguments.friendPaths?.toList())
        configuration.putIfNotNull(K2MetadataConfigurationKeys.REFINES_PATHS, arguments.refinesPaths?.toList())

        configuration.perfManager!!.apply {
            val mode = if (arguments.metadataKlib) "KLib" else "metadata"
            targetDescription = "$mode mode for $moduleName module"
        }

        val destination = arguments.destination
        if (destination != null) {
            if (destination.endsWith(".jar")) {
                // TODO: support .jar destination
                collector.report(
                    STRONG_WARNING,
                    ".jar destination is not yet supported, results will be written to the directory with the given name"
                )
            }
            configuration.put(CLIConfigurationKeys.METADATA_DESTINATION_DIRECTORY, File(destination))
        } else {
            collector.report(ERROR, "Specify destination via -d")
        }
    }
}
