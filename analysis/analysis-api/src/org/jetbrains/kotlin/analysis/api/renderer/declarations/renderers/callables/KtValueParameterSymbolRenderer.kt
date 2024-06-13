/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KaValueParameterSymbolRenderer {
    public fun renderSymbol(
        analysisSession: KaSession,
        symbol: KaValueParameterSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object AS_SOURCE : KaValueParameterSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KaSession,
            symbol: KaValueParameterSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                " = ".separated(
                    {
                        declarationRenderer.callableSignatureRenderer
                            .renderCallableSignature(analysisSession, symbol, keyword = null, declarationRenderer, printer)
                    },
                    { declarationRenderer.parameterDefaultValueRenderer.renderDefaultValue(analysisSession, symbol, printer) },
                )
            }
        }
    }

    public object TYPE_ONLY : KaValueParameterSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KaSession,
            symbol: KaValueParameterSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                declarationRenderer.typeRenderer.renderType(analysisSession, symbol.returnType, printer)
            }
        }
    }
}

public typealias KtValueParameterSymbolRenderer = KaValueParameterSymbolRenderer