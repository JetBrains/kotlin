/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.types.Variance

public interface KaCallableReturnTypeRenderer {
    public fun renderReturnType(
        analysisSession: KaSession,
        symbol: KaCallableSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object WITH_OUT_APPROXIMATION : KaCallableReturnTypeRenderer {
        override fun renderReturnType(
            analysisSession: KaSession,
            symbol: KaCallableSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            if (symbol is KaConstructorSymbol) return
            val type = declarationRenderer.declarationTypeApproximator.approximateType(analysisSession, symbol.returnType, Variance.OUT_VARIANCE)
            if (!declarationRenderer.returnTypeFilter.shouldRenderReturnType(analysisSession, type, symbol)) return
            declarationRenderer.typeRenderer.renderType(analysisSession, type, printer)
        }
    }
}

public typealias KtCallableReturnTypeRenderer = KaCallableReturnTypeRenderer