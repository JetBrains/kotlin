/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.loader

import org.jetbrains.kotlin.idea.core.script.debug
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.LegacyResolverWrapper
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import org.jetbrains.kotlin.scripting.resolve.refineScriptCompilationConfiguration
import kotlin.script.experimental.dependencies.AsyncDependenciesResolver

class FromRefinedConfigurationLoader internal constructor() :
    ScriptDependenciesLoader {
    override fun isAsync(file: KtFile, scriptDefinition: ScriptDefinition): Boolean {
        return scriptDefinition.asLegacyOrNull<KotlinScriptDefinition>()?.dependencyResolver?.let {
            it is AsyncDependenciesResolver || it is LegacyResolverWrapper
        } ?: false
    }

    override fun loadDependencies(
        firstLoad: Boolean,
        file: KtFile,
        scriptDefinition: ScriptDefinition
    ): ScriptCompilationConfigurationResult? {
        debug(file) { "start dependencies loading" }

        val result = refineScriptCompilationConfiguration(KtFileScriptSource(file), scriptDefinition, file.project)

        debug(file) { "finish dependencies loading" }

        return result
    }
}


