/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirScriptConfiguratorExtensionImpl
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirScriptDefinitionProviderService
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionsSource
import org.jetbrains.kotlin.scripting.definitions.ScriptDependenciesProvider
import kotlin.script.experimental.host.ScriptingHostConfiguration

/**
 * The class is a copy of [org.jetbrains.kotlin.scripting.compiler.plugin.FirScriptingCompilerExtensionRegistrar] adapted for the usage
 * outside the compiler context. Difference lies in its constructor parameters, namely in
 * [org.jetbrains.kotlin.config.CompilerConfiguration] replaced with a pair of explicit [scriptDefinitionSources] and [scriptDefinitions].
 */
internal class FirScriptingCompilerExtensionIdeRegistrar(
    private val project: Project,
    private val hostConfiguration: ScriptingHostConfiguration,
    private val scriptDefinitionSources: List<ScriptDefinitionsSource>,
    private val scriptDefinitions: List<ScriptDefinition>
) : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        val definitionSources = scriptDefinitionSources
        val definitions = scriptDefinitions
        if (definitionSources.isNotEmpty() || definitions.isNotEmpty()) {
            +FirScriptDefinitionProviderService.getFactory(
                definitions, definitionSources,
                ScriptDefinitionProvider.getInstance(project),
                ScriptDependenciesProvider.getInstance(project)
            )
        }

        +FirScriptConfiguratorExtensionImpl.getFactory(hostConfiguration)
    }
}