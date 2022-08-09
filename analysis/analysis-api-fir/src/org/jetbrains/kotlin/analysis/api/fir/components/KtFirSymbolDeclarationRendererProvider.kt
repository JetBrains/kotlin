/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KtDeclarationRendererOptions
import org.jetbrains.kotlin.analysis.api.components.KtSymbolDeclarationRendererProvider
import org.jetbrains.kotlin.analysis.api.components.KtTypeRendererOptions
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.renderer.ConeTypeIdeRenderer
import org.jetbrains.kotlin.analysis.api.fir.renderer.FirIdeRenderer
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.types.KtFirType
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase

internal class KtFirSymbolDeclarationRendererProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken
) : KtSymbolDeclarationRendererProvider() {

    override fun render(type: KtType, options: KtTypeRendererOptions): String {
        require(type is KtFirType)
        return ConeTypeIdeRenderer(analysisSession.firResolveSession.useSiteFirSession, options).renderType(type.coneType)
    }

    override fun renderDeclaration(symbol: KtDeclarationSymbol, options: KtDeclarationRendererOptions): String {
        require(symbol is KtFirSymbol<*>)
        symbol.firSymbol.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
        return FirIdeRenderer.render(symbol.firSymbol.fir, options, symbol.firSymbol.fir.moduleData.session)
    }
}
