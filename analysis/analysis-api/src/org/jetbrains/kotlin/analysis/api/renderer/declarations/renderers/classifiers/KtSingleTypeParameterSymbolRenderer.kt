/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.classifiers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.types.Variance

public interface KtSingleTypeParameterSymbolRenderer {
    public fun renderSymbol(
        analysisSession: KtAnalysisSession,
        symbol: KtTypeParameterSymbol,
        declarationRenderer: KtDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object NO : KtSingleTypeParameterSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KtAnalysisSession,
            symbol: KtTypeParameterSymbol,
            declarationRenderer: KtDeclarationRenderer,
            printer: PrettyPrinter,
        ) {}
    }


    public object WITHOUT_BOUNDS : KtSingleTypeParameterSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KtAnalysisSession,
            symbol: KtTypeParameterSymbol,
            declarationRenderer: KtDeclarationRenderer,
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

    public object WITH_COMMA_SEPARATED_BOUNDS : KtSingleTypeParameterSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KtAnalysisSession,
            symbol: KtTypeParameterSymbol,
            declarationRenderer: KtDeclarationRenderer,
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
