/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.CachingKtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getResolveState
import org.jetbrains.kotlin.psi.KtElement

@OptIn(InvalidWayOfUsingAnalysisSession::class)
class KtFirAnalysisSessionProvider(project: Project) : CachingKtAnalysisSessionProvider<LLFirModuleResolveState>(project) {
    override fun getResolveState(contextElement: KtElement): LLFirModuleResolveState {
        return contextElement.getResolveState()
    }

    override fun getResolveState(contextSymbol: KtSymbol): LLFirModuleResolveState {
        return when (contextSymbol) {
            is KtFirSymbol<*> -> contextSymbol.resolveState
            else -> error("Invalid symbol ${contextSymbol::class}")
        }
    }

    override fun createAnalysisSession(
        resolveState: LLFirModuleResolveState,
        validityToken: ValidityToken,
    ): KtAnalysisSession {
        @Suppress("DEPRECATION")
        return KtFirAnalysisSession.createAnalysisSessionByResolveState(resolveState, validityToken)
    }
}


