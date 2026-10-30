/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.scripting

import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirScriptExtensionRegistrarProvider
import org.jetbrains.kotlin.assignment.plugin.AssignmentCommandLineProcessor
import org.jetbrains.kotlin.assignment.plugin.AssignmentConfigurationKeys
import org.jetbrains.kotlin.assignment.plugin.k2.FirAssignmentPluginExtensionRegistrar
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.plugins.processCompilerPluginsOptions
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.compiler.plugin.FirScriptingSamWithReceiverExtensionRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.makeScriptCompilerArguments
import org.jetbrains.kotlin.scripting.definitions.K1SpecificScriptingServiceAccessor
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withVirtualFileEntry
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

internal class LLFirScriptingExtensionRegistrarProvider : LLFirScriptExtensionRegistrarProvider {
    @OptIn(ExperimentalCompilerApi::class)
    override fun getExtensionRegistrars(module: KaScriptModule): List<FirExtensionRegistrar> {
        val scriptDefinition = findScriptDefinition(module.file)

        val compilerArguments = makeScriptCompilerArguments(scriptDefinition.compilerOptions.toList())
        val commandLineProcessors = listOf(AssignmentCommandLineProcessor())
        val compilerConfiguration = CompilerConfiguration.create()
        processCompilerPluginsOptions(
            compilerConfiguration, compilerArguments.pluginOptions.asIterable(), commandLineProcessors
        )

        return buildList {
            add(
                FirScriptingCompilerExtensionIdeRegistrar(
                    module.project,
                    scriptDefinition.hostConfiguration,
                    scriptDefinitionSources = emptyList(),
                    scriptDefinitions = listOf(scriptDefinition)
                )
            )
            add(FirScriptingSamWithReceiverExtensionRegistrar())

            compilerConfiguration.getList(AssignmentConfigurationKeys.ASSIGNMENT_ANNOTATION).takeIf { it.isNotEmpty() }?.let {
                add(FirAssignmentPluginExtensionRegistrar(it))
            }
        }
    }

    @OptIn(K1SpecificScriptingServiceAccessor::class)
    private fun findScriptDefinition(file: KtFile): ScriptDefinition {
        // Standalone doesn't require the platform to provide script definitions
        if (ScriptDefinitionProvider.getInstance(file.project) == null) {
            return ScriptDefinition.getDefault(defaultJvmScriptingHostConfiguration)
        }

        return file.findScriptDefinition() ?: errorWithAttachment("Cannot load script definition") {
            withVirtualFileEntry("file", file.virtualFile)
        }
    }
}
