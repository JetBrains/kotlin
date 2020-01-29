/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.loader

import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.cache.CachedConfigurationInputs
import org.jetbrains.kotlin.idea.core.script.configuration.cache.ScriptConfigurationSnapshot
import org.jetbrains.kotlin.idea.core.script.debug
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.LegacyResolverWrapper
import org.jetbrains.kotlin.scripting.resolve.refineScriptCompilationConfiguration
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.asDiagnostics
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.dependencies.AsyncDependenciesResolver

open class DefaultScriptConfigurationLoader(val project: Project) : ScriptConfigurationLoader {
    override fun shouldRunInBackground(scriptDefinition: ScriptDefinition): Boolean =
        scriptDefinition
            .asLegacyOrNull<KotlinScriptDefinition>()
            ?.dependencyResolver
            ?.let { it is AsyncDependenciesResolver || it is LegacyResolverWrapper }
            ?: false

    override fun loadDependencies(
        isFirstLoad: Boolean,
        ktFile: KtFile,
        scriptDefinition: ScriptDefinition,
        context: ScriptConfigurationLoadingContext
    ): Boolean {
        val virtualFile = ktFile.originalFile.virtualFile

        debug(ktFile) { "start dependencies loading" }

        val inputs = getInputsStamp(virtualFile, ktFile)
        val scriptingApiResult = try {
            refineScriptCompilationConfiguration(
                KtFileScriptSource(ktFile), scriptDefinition, ktFile.project
            )
        } catch (e: Throwable) {
            if (e is ControlFlowException) throw e

            ResultWithDiagnostics.Failure(listOf(e.asDiagnostics()))
        }

        val result = ScriptConfigurationSnapshot(
            inputs,
            scriptingApiResult.reports,
            scriptingApiResult.valueOrNull()
        )

        context.suggestNewConfiguration(virtualFile, result)

        debug(ktFile) { "finish dependencies loading" }

        return true
    }

    protected open fun getInputsStamp(virtualFile: VirtualFile, file: KtFile): CachedConfigurationInputs {
        return CachedConfigurationInputs.PsiModificationStamp.get(project, virtualFile, file)
    }
}