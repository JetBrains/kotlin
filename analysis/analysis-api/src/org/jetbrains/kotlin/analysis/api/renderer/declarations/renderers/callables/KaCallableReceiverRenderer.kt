/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.types.Variance

@KaExperimentalApi
public interface KaCallableReceiverRenderer {
    public fun renderReceiver(
        analysisSession: KaSession,
        symbol: KaReceiverParameterSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object AS_TYPE_WITH_IN_APPROXIMATION : KaCallableReceiverRenderer {
        override fun renderReceiver(
            analysisSession: KaSession,
            symbol: KaReceiverParameterSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                " ".separated(
                    {
                        declarationRenderer.annotationRenderer.renderAnnotations(analysisSession, symbol, printer)
                    },
                    {
                        val receiverType = declarationRenderer.declarationTypeApproximator
                            .approximateType(analysisSession, symbol.returnType, Variance.IN_VARIANCE)

                        declarationRenderer.typeRenderer.renderType(analysisSession, receiverType, printer)
                    },
                )
            }
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaCallableReceiverRenderer' instead", ReplaceWith("KaCallableReceiverRenderer"))
public typealias KtCallableReceiverRenderer = KaCallableReceiverRenderer