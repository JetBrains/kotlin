/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaSymbolDeclarationRendererProvider
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaRendererTypeApproximator
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.types.Variance

@KaAnalysisApiInternals
open class KaRendererProviderImpl(
    override val analysisSession: KaSession,
    override val token: KaLifetimeToken
) : KaSymbolDeclarationRendererProvider() {

    override fun renderType(type: KaType, renderer: KaTypeRenderer, position: Variance): String {
        return with(analysisSession) {
            val approximatedType = KaRendererTypeApproximator.TO_DENOTABLE.approximateType(analysisSession, type, position)
            prettyPrint { renderer.renderType(analysisSession, approximatedType, this) }
        }
    }

    override fun renderDeclaration(symbol: KaDeclarationSymbol, renderer: KaDeclarationRenderer): String {
        return with(analysisSession) {
            prettyPrint { renderer.renderDeclaration(analysisSession, symbol, this) }
        }
    }
}