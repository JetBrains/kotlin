/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.types.Variance

public interface KtCallableReceiverRenderer {
    public fun renderReceiver(
        analysisSession: KtAnalysisSession,
        symbol: KtReceiverParameterSymbol,
        declarationRenderer: KtDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object AS_TYPE_WITH_IN_APPROXIMATION : KtCallableReceiverRenderer {
        override fun renderReceiver(
            analysisSession: KtAnalysisSession,
            symbol: KtReceiverParameterSymbol,
            declarationRenderer: KtDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                " ".separated(
                    {
                        declarationRenderer.annotationRenderer.renderAnnotations(analysisSession, symbol, printer)
                    },
                    {
                        val receiverType = declarationRenderer.declarationTypeApproximator
                            .approximateType(analysisSession, symbol.type, Variance.IN_VARIANCE)

                        declarationRenderer.typeRenderer.renderType(analysisSession, receiverType, printer)
                    },
                )
            }
        }
    }
}
