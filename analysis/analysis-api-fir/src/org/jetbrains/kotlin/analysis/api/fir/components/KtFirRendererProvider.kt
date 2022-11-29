/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.components.KtRendererProviderImpl
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase

@OptIn(KtAnalysisApiInternals::class)
internal class KtFirRendererProvider(
    analysisSession: KtAnalysisSession,
    token: KtLifetimeToken
) : KtRendererProviderImpl(analysisSession, token) {
    override fun renderDeclaration(symbol: KtDeclarationSymbol, renderer: KtDeclarationRenderer): String {
        symbol.firSymbol.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
        return super.renderDeclaration(symbol, renderer)
    }
}