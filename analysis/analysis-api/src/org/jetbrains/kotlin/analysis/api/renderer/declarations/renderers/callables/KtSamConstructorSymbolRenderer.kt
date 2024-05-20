/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaSamConstructorSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KaSamConstructorSymbolRenderer {
    public fun renderSymbol(
        analysisSession: KaSession,
        symbol: KaSamConstructorSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object NOT_RENDER : KaSamConstructorSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KaSession,
            symbol: KaSamConstructorSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {}
        }
    }

    public object AS_FUNCTION : KaSamConstructorSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KaSession,
            symbol: KaSamConstructorSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                declarationRenderer.callableSignatureRenderer
                    .renderCallableSignature(analysisSession, symbol, keyword = null, declarationRenderer, printer)
            }
        }
    }
}

public typealias KtSamConstructorSymbolRenderer = KaSamConstructorSymbolRenderer