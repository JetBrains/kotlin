/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.classifiers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.types.Variance

@KaExperimentalApi
public interface KaSingleTypeParameterSymbolRenderer {
    public fun renderSymbol(
        analysisSession: KaSession,
        symbol: KaTypeParameterSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    @KaExperimentalApi
    public object NO : KaSingleTypeParameterSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KaSession,
            symbol: KaTypeParameterSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {}
    }

    @KaExperimentalApi
    public object WITHOUT_BOUNDS : KaSingleTypeParameterSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KaSession,
            symbol: KaTypeParameterSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                " ".separated(
                    { declarationRenderer.annotationRenderer.renderAnnotations(analysisSession, symbol, printer) },
                    { declarationRenderer.modifiersRenderer.renderDeclarationModifiers(analysisSession, symbol, printer) },
                    { declarationRenderer.nameRenderer.renderName(analysisSession, symbol, declarationRenderer, printer) },
                )
            }
        }
    }

    @KaExperimentalApi
    public object WITH_COMMA_SEPARATED_BOUNDS : KaSingleTypeParameterSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KaSession,
            symbol: KaTypeParameterSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                " ".separated(
                    { declarationRenderer.annotationRenderer.renderAnnotations(analysisSession, symbol, printer) },
                    { declarationRenderer.modifiersRenderer.renderDeclarationModifiers(analysisSession, symbol, printer) },
                    { declarationRenderer.nameRenderer.renderName(analysisSession, symbol,declarationRenderer, printer) },
                    {
                        if (symbol.upperBounds.isNotEmpty()) {
                            withPrefix(": ") {
                                printCollection(symbol.upperBounds) {
                                    val approximatedType = declarationRenderer.declarationTypeApproximator
                                        .approximateType(analysisSession, it, Variance.OUT_VARIANCE)

                                    declarationRenderer.typeRenderer.renderType(analysisSession, approximatedType, printer)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
