/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.superTypes

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

@KaExperimentalApi
public interface KaSuperTypeListRenderer {
    public fun renderSuperTypes(
        analysisSession: KaSession,
        symbol: KaClassSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    @KaExperimentalApi
    public object AS_LIST : KaSuperTypeListRenderer {
        override fun renderSuperTypes(
            analysisSession: KaSession,
            symbol: KaClassSymbol,
            declarationRenderer: KaDeclarationRenderer,
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
