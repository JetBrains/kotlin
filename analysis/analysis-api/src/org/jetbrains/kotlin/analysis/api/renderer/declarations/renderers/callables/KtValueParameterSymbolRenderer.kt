/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KtValueParameterSymbolRenderer {
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderSymbol(symbol: KtValueParameterSymbol, printer: PrettyPrinter)

    public object AS_SOURCE : KtValueParameterSymbolRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSymbol(symbol: KtValueParameterSymbol, printer: PrettyPrinter): Unit = printer {
            " = ".separated(
                { callableSignatureRenderer.renderCallableSignature(symbol, keyword = null, printer) },
                { parameterDefaultValueRenderer.renderDefaultValue(symbol, printer) },
            )
        }
    }

    public object TYPE_ONLY : KtValueParameterSymbolRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSymbol(symbol: KtValueParameterSymbol, printer: PrettyPrinter): Unit = printer {
            typeRenderer.renderType(symbol.returnType, printer)
        }
    }
}