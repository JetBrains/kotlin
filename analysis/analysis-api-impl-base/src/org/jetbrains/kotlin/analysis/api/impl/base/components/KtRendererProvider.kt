/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.KtSymbolDeclarationRendererProvider
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtRendererTypeApproximator
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.types.Variance

@KtAnalysisApiInternals
open class KtRendererProviderImpl(
    override val analysisSession: KtAnalysisSession,
    override val token: KtLifetimeToken
) : KtSymbolDeclarationRendererProvider() {

    override fun renderType(type: KtType, renderer: KtTypeRenderer, position: Variance): String {
        return with(analysisSession) {
            val approximatedType = KtRendererTypeApproximator.TO_DENOTABLE.approximateType(type, position)
            prettyPrint { renderer.renderType(approximatedType, this) }
        }
    }

    override fun renderDeclaration(symbol: KtDeclarationSymbol, renderer: KtDeclarationRenderer): String {
        return with(analysisSession) {
            prettyPrint { renderer.renderDeclaration(symbol, this) }
        }
    }
}