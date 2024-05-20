/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.superTypes

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.types.Variance

public interface KaSuperTypeRenderer {
    public fun renderSuperType(
        analysisSession: KaSession,
        type: KaType,
        symbol: KaClassOrObjectSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object WITH_OUT_APPROXIMATION : KaSuperTypeRenderer {
        override fun renderSuperType(
            analysisSession: KaSession,
            type: KaType,
            symbol: KaClassOrObjectSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            val approximatedType = declarationRenderer.declarationTypeApproximator
                .approximateType(analysisSession, type, Variance.OUT_VARIANCE)

            declarationRenderer.typeRenderer.renderType(analysisSession, approximatedType, printer)
        }
    }
}

public typealias KtSuperTypeRenderer = KaSuperTypeRenderer