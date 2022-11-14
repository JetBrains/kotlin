/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.receiverType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.types.Variance

public interface KtCallableReceiverTypeRenderer {
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderReceiverType(symbol: KtCallableSymbol, printer: PrettyPrinter)

    public object WITH_IN_APPROXIMATION : KtCallableReceiverTypeRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderReceiverType(symbol: KtCallableSymbol, printer: PrettyPrinter) {
            val receiverType = symbol.receiverType?.let { declarationTypeApproximator.approximateType(it, Variance.IN_VARIANCE) } ?: return
            typeRenderer.renderType(receiverType, printer)
        }
    }
}