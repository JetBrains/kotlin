/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderAnnotationsModifiersAndContextReceivers
import org.jetbrains.kotlin.analysis.api.symbols.KaClassInitializerSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens

@KaExperimentalApi
public interface KaClassInitializerRenderer {
    public fun renderClassInitializer(
        analysisSession: KaSession,
        symbol: KaClassInitializerSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    @KaExperimentalApi
    public object INIT_BLOCK_WITH_BRACES : KaClassInitializerRenderer {
        override fun renderClassInitializer(
            analysisSession: KaSession,
            symbol: KaClassInitializerSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                " ".separated(
                    {
                        renderAnnotationsModifiersAndContextReceivers(
                            analysisSession,
                            symbol,
                            declarationRenderer,
                            printer,
                            KtTokens.INIT_KEYWORD,
                        )
                    },
                    { printer.withIndentInBraces {} },
                )
            }
        }
    }
}
