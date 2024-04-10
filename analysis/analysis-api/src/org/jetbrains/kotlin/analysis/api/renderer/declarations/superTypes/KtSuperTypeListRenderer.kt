/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.superTypes

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KtSuperTypeListRenderer {
    public fun renderSuperTypes(
        analysisSession: KtAnalysisSession,
        symbol: KtClassOrObjectSymbol,
        declarationRenderer: KtDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object AS_LIST : KtSuperTypeListRenderer {
        override fun renderSuperTypes(
            analysisSession: KtAnalysisSession,
            symbol: KtClassOrObjectSymbol,
            declarationRenderer: KtDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                val superTypesToRender = symbol.superTypes
                    .filter { declarationRenderer.superTypesFilter.filter(analysisSession, it, symbol) }.ifEmpty { return }

                printCollection(superTypesToRender) { type ->
                    declarationRenderer.superTypeRenderer.renderSuperType(analysisSession, type, symbol, declarationRenderer, printer)
                    declarationRenderer.superTypesArgumentRenderer
                        .renderSuperTypeArguments(analysisSession, type, symbol, declarationRenderer, printer)
                }
            }
        }
    }
}
