/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.classifiers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderAnnotationsModifiersAndContextReceivers
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens

@KaExperimentalApi
public interface KaTypeAliasSymbolRenderer {
    public fun renderSymbol(
        analysisSession: KaSession,
        symbol: KaTypeAliasSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object AS_SOURCE : KaTypeAliasSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KaSession,
            symbol: KaTypeAliasSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                " ".separated(
                    {
                        renderAnnotationsModifiersAndContextReceivers(analysisSession, symbol, declarationRenderer, printer, KtTokens.TYPE_ALIAS_KEYWORD)
                    },
                    {
                        " = ".separated(
                            {
                                declarationRenderer.nameRenderer.renderName(analysisSession, symbol, declarationRenderer, printer)

                                declarationRenderer.typeParametersRenderer
                                    .renderTypeParameters(analysisSession, symbol, declarationRenderer, printer)
                            },
                            { declarationRenderer.typeRenderer.renderType(analysisSession, symbol.expandedType, printer) })
                    }
                )
            }
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaTypeAliasSymbolRenderer' instead", ReplaceWith("KaTypeAliasSymbolRenderer"))
public typealias KtTypeAliasSymbolRenderer = KaTypeAliasSymbolRenderer