/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.types.Variance

public interface KtCallableReturnTypeRenderer {
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderReturnType(symbol: KtCallableSymbol, printer: PrettyPrinter)

    public object WITH_OUT_APPROXIMATION : KtCallableReturnTypeRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderReturnType(symbol: KtCallableSymbol, printer: PrettyPrinter) {
            if (symbol is KtConstructorSymbol) return
            val type = declarationTypeApproximator.approximateType(symbol.returnType, Variance.OUT_VARIANCE)
            if (!returnTypeFilter.shouldRenderReturnType(type, symbol)) return
            typeRenderer.renderType(type, printer)
        }
    }
}