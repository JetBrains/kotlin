/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
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
            return ServiceManager.getService(project, Fe10AnalysisFacade::class.java)
        }
    }

    fun getComponentProvider(element: KtElement): Fe10ComponentProvider

    fun analyze(element: KtElement, mode: AnalysisMode = AnalysisMode.FULL): BindingContext

    fun getOrigin(file: VirtualFile): KtSymbolOrigin

    enum class AnalysisMode {
        FULL_WITH_ALL_CHECKS,
        FULL,
        PARTIAL_WITH_DIAGNOSTICS,
        PARTIAL
    }
}

interface Fe10ComponentProvider {
    val resolveSession: ResolveSession
    val deprecationResolver: DeprecationResolver
    val callResolver: CallResolver
    val kotlinToResolvedCallTransformer: KotlinToResolvedCallTransformer
    val overloadingConflictResolver: OverloadingConflictResolver<ResolvedCall<*>>
    val kotlinTypeRefiner: KotlinTypeRefiner
}

class Fe10AnalysisContext(
    facade: Fe10AnalysisFacade,
    contextElement: KtElement,
    val token: KtLifetimeToken
) : Fe10AnalysisFacade by facade, Fe10ComponentProvider by facade.getComponentProvider(contextElement) {
    val builtIns: KotlinBuiltIns
        get() = resolveSession.moduleDescriptor.builtIns

    val languageVersionSettings: LanguageVersionSettings
        get() = resolveSession.languageVersionSettings
}
