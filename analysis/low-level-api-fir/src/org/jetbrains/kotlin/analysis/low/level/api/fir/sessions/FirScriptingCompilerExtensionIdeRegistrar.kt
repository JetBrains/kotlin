/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.*
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirScriptConfiguratorExtensionImpl
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirScriptDefinitionProviderService
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirScriptResolutionConfigurationExtensionImpl
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.with

/**
 * The class is a copy of [org.jetbrains.kotlin.scripting.compiler.plugin.FirScriptingCompilerExtensionRegistrar] adapted for the usage
 * outside the compiler context. Difference lies in its constructor parameters, namely in
 * [org.jetbrains.kotlin.config.CompilerConfiguration] replaced with a pair of explicit [scriptDefinitionSources] and [scriptDefinitions].
 */
internal class FirScriptingCompilerExtensionIdeRegistrar(
    private val project: Project,
    private val hostConfiguration: ScriptingHostConfiguration,
    @Suppress("DEPRECATION") //KT-82551
    private val scriptDefinitionSources: List<org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionsSource>,
    private val scriptDefinitions: List<ScriptDefinition>,
) : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        val legacyDefinitionsProvider = ScriptDefinitionProvider.getInstance(project) ?: run {
            if (scriptDefinitionSources.isNotEmpty() || scriptDefinitions.isNotEmpty()) {
                CliScriptDefinitionProvider().also {
                    it.setScriptDefinitionsSources(scriptDefinitionSources)
                    it.setScriptDefinitions(scriptDefinitions)
                }
            } else return
        }
        +FirScriptDefinitionProviderService.getFactory {
            hostConfiguration.with {
                scriptCompilationConfigurationProvider(
                    ScriptCompilationConfigurationProviderOverDefinitionProvider(legacyDefinitionsProvider)
                )
                scriptRefinedCompilationConfigurationsCache(ScriptRefinedCompilationConfigurationCacheImpl())
            }
        }

        +FirScriptConfiguratorExtensionImpl.getFactory()
        +FirScriptResolutionConfigurationExtensionImpl.getFactory()
    }
}