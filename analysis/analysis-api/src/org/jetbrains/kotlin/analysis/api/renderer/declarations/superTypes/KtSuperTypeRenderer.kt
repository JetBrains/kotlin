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
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderSuperType(type: KtType, symbol: KtClassOrObjectSymbol, printer: PrettyPrinter)

    public object WITH_OUT_APPROXIMATION : KtSuperTypeRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSuperType(type: KtType, symbol: KtClassOrObjectSymbol, printer: PrettyPrinter) {
            typeRenderer.renderType(declarationTypeApproximator.approximateType(type, Variance.OUT_VARIANCE), printer)
        }
    }
}