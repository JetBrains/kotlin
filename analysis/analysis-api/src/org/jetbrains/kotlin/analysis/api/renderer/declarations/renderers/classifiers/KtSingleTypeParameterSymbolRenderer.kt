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
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderSymbol(symbol: KtTypeParameterSymbol, printer: PrettyPrinter)

    public object NO : KtSingleTypeParameterSymbolRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSymbol(symbol: KtTypeParameterSymbol, printer: PrettyPrinter) {
        }
    }


    public object WITHOUT_BOUNDS : KtSingleTypeParameterSymbolRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSymbol(symbol: KtTypeParameterSymbol, printer: PrettyPrinter): Unit = printer {
            " ".separated(
                { annotationRenderer.renderAnnotations(symbol, printer) },
                { modifiersRenderer.renderDeclarationModifiers(symbol, printer) },
                { nameRenderer.renderName(symbol, printer) },
            )
        }
    }

    public object WITH_COMMA_SEPARATED_BOUNDS : KtSingleTypeParameterSymbolRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSymbol(symbol: KtTypeParameterSymbol, printer: PrettyPrinter): Unit = printer {
            " ".separated(
                { annotationRenderer.renderAnnotations(symbol, printer) },
                { modifiersRenderer.renderDeclarationModifiers(symbol, printer) },
                { nameRenderer.renderName(symbol, printer) },
                {
                    if (symbol.upperBounds.isNotEmpty()) {
                        withPrefix(": ") {
                            printCollection(symbol.upperBounds) {
                                typeRenderer.renderType(declarationTypeApproximator.approximateType(it, Variance.OUT_VARIANCE), printer)
                            }
                        }
                    }
                }
            )
        }
    }
}
