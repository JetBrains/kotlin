/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.frontend.api.components.KtDeclarationRendererOptions
import org.jetbrains.kotlin.idea.frontend.api.components.KtSymbolDeclarationRendererProvider
import org.jetbrains.kotlin.idea.frontend.api.components.KtTypeRendererOptions
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.renderer.ConeTypeIdeRenderer
import org.jetbrains.kotlin.idea.frontend.api.fir.renderer.FirIdeRenderer
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.types.KtFirType
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithKind
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.types.KtType

internal class KtFirSymbolDeclarationRendererProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtSymbolDeclarationRendererProvider() {

    override fun render(type: KtType, options: KtTypeRendererOptions): String {
        require(type is KtFirType)
        return ConeTypeIdeRenderer(analysisSession.firResolveState.rootModuleSession, options).renderType(type.coneType)
    }

    override fun render(symbol: KtSymbol, options: KtDeclarationRendererOptions): String {
        require(symbol is KtFirSymbol<*>)
        val containingSymbol = with(analysisSession) {
            (symbol as? KtSymbolWithKind)?.getContainingSymbol()
        }
        check(containingSymbol is KtFirSymbol<*>?)

        val phaseNeeded =
            if (options.renderContainingDeclarations) FirResolvePhase.BODY_RESOLVE else FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE

        return (containingSymbol ?: symbol).firRef.withFir(phaseNeeded) { fir ->
            val containingFir = containingSymbol?.firRef?.withFirUnsafe { it }
            FirIdeRenderer.render(fir, containingFir, options, fir.session)
        }
    }
}