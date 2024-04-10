/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.superTypes

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.types.Variance

public interface KtSuperTypeRenderer {
    public fun renderSuperType(
        analysisSession: KtAnalysisSession,
        type: KtType,
        symbol: KtClassOrObjectSymbol,
        declarationRenderer: KtDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object WITH_OUT_APPROXIMATION : KtSuperTypeRenderer {
        override fun renderSuperType(
            analysisSession: KtAnalysisSession,
            type: KtType,
            symbol: KtClassOrObjectSymbol,
            declarationRenderer: KtDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            val approximatedType = declarationRenderer.declarationTypeApproximator
                .approximateType(analysisSession, type, Variance.OUT_VARIANCE)

            declarationRenderer.typeRenderer.renderType(analysisSession, approximatedType, printer)
        }
    }
}