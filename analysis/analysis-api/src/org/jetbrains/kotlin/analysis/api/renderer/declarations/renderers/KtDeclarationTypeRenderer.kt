/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.types.Variance

// TODO separate into components
public interface KtDeclarationTypeRenderer {
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderReturnType(symbol: KtCallableSymbol, printer: PrettyPrinter)

    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderReceiverType(symbol: KtCallableSymbol, printer: PrettyPrinter)

    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderSuperType(type: KtType, symbol: KtClassOrObjectSymbol, printer: PrettyPrinter)


    public object NO_IMPLICIT_TYPES : KtDeclarationTypeRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderReturnType(symbol: KtCallableSymbol, printer: PrettyPrinter) {
            if (symbol is KtConstructorSymbol) return
            val type = declarationTypeApproximator.approximateType(symbol.returnType, Variance.OUT_VARIANCE)
            if (!returnTypeFilter.shouldRenderReturnType(type, symbol)) return
            typeRenderer.renderType(type, printer)
        }

        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderReceiverType(symbol: KtCallableSymbol, printer: PrettyPrinter) {
            val receiverType = symbol.receiverType?.let { declarationTypeApproximator.approximateType(it, Variance.IN_VARIANCE) } ?: return
            typeRenderer.renderType(receiverType, printer)
        }

        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSuperType(type: KtType, symbol: KtClassOrObjectSymbol, printer: PrettyPrinter) {
            typeRenderer.renderType(declarationTypeApproximator.approximateType(type, Variance.OUT_VARIANCE), printer)
        }
    }
}