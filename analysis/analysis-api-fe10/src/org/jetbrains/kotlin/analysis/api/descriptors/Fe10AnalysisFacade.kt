/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.results.OverloadingConflictResolver
import org.jetbrains.kotlin.resolve.calls.tower.KotlinToResolvedCallTransformer
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner

interface Fe10AnalysisFacade {
    companion object {
        fun getInstance(project: Project): Fe10AnalysisFacade {
            return project.getService(Fe10AnalysisFacade::class.java)
        }
    }

    fun getAnalysisContext(element: KtElement, token: KtLifetimeToken): Fe10AnalysisContext

    fun getAnalysisContext(ktModule: KtModule, token: KtLifetimeToken): Fe10AnalysisContext

    fun analyze(elements: List<KtElement>, mode: AnalysisMode = AnalysisMode.FULL): BindingContext

    fun analyze(element: KtElement, mode: AnalysisMode = AnalysisMode.FULL): BindingContext {
        return analyze(listOf(element), mode)
    }

    fun getOrigin(file: VirtualFile): KtSymbolOrigin

    enum class AnalysisMode {
        ALL_COMPILER_CHECKS,
        FULL,
        PARTIAL_WITH_DIAGNOSTICS,
        PARTIAL
    }
}

class Fe10AnalysisContext(
    facade: Fe10AnalysisFacade,
    val resolveSession: ResolveSession,
    val deprecationResolver: DeprecationResolver,
    val callResolver: CallResolver,
    val kotlinToResolvedCallTransformer: KotlinToResolvedCallTransformer,
    val overloadingConflictResolver: OverloadingConflictResolver<ResolvedCall<*>>,
    val kotlinTypeRefiner: KotlinTypeRefiner,
    val token: KtLifetimeToken,
) : Fe10AnalysisFacade by facade {
    val builtIns: KotlinBuiltIns
        get() = resolveSession.moduleDescriptor.builtIns

    val languageVersionSettings: LanguageVersionSettings
        get() = resolveSession.languageVersionSettings
}
