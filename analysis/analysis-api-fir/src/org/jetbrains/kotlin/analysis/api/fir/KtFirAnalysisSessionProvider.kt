/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.CachingKtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getResolveState
import org.jetbrains.kotlin.psi.KtElement

@OptIn(InvalidWayOfUsingAnalysisSession::class)
class KtFirAnalysisSessionProvider(project: Project) : CachingKtAnalysisSessionProvider<FirModuleResolveState>(project) {
    override fun getResolveState(contextElement: KtElement): FirModuleResolveState {
        return contextElement.getResolveState()
    }

    override fun getResolveState(contextSymbol: KtSymbol): FirModuleResolveState {
        require(contextSymbol is KtFirSymbol<*>)
        return contextSymbol.firRef.resolveState
    }

    override fun createAnalysisSession(
        resolveState: FirModuleResolveState,
        validityToken: ValidityToken,
        contextElement: KtElement
    ): KtAnalysisSession {
        @Suppress("DEPRECATION")
        return KtFirAnalysisSession.createAnalysisSessionByResolveState(resolveState, validityToken, contextElement)
    }
}