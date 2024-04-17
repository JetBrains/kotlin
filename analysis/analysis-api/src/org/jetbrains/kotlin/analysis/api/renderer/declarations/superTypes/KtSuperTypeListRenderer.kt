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
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderSuperTypes(symbol: KtClassOrObjectSymbol, printer: PrettyPrinter)

    public object AS_LIST : KtSuperTypeListRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSuperTypes(symbol: KtClassOrObjectSymbol, printer: PrettyPrinter): Unit = printer {
            val superTypesToRender = symbol.superTypes.filter { superTypesFilter.filter(it, symbol) }.ifEmpty { return }
            printCollection(superTypesToRender) { type ->
                superTypeRenderer.renderSuperType(type, symbol, printer)
                superTypesArgumentRenderer.renderSuperTypeArguments(type, symbol, printer)
            }
        }
    }
}
