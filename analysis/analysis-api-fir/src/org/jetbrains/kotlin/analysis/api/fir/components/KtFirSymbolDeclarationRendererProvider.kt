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
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase

internal class KtFirSymbolDeclarationRendererProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtSymbolDeclarationRendererProvider() {

    override fun render(type: KtType, options: KtTypeRendererOptions): String {
        require(type is KtFirType)
        return ConeTypeIdeRenderer(analysisSession.firResolveState.rootModuleSession, options).renderType(type.coneType)
    }

    override fun renderDeclaration(symbol: KtDeclarationSymbol, options: KtDeclarationRendererOptions): String {
        require(symbol is KtFirSymbol<*>)
        return symbol.firRef.withFir(FirResolvePhase.BODY_RESOLVE) { fir ->
            FirIdeRenderer.render(fir, options, fir.moduleData.session)
        }
    }
}
