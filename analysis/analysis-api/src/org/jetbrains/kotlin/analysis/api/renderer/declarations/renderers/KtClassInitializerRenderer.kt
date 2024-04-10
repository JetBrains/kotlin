/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtClassInitializerSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens

public interface KtClassInitializerRenderer {
    public fun renderClassInitializer(
        analysisSession: KtAnalysisSession,
        symbol: KtClassInitializerSymbol,
        declarationRenderer: KtDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object INIT_BLOCK_WITH_BRACES : KtClassInitializerRenderer {
        override fun renderClassInitializer(
            analysisSession: KtAnalysisSession,
            symbol: KtClassInitializerSymbol,
            declarationRenderer: KtDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                " ".separated(
                    { declarationRenderer.keywordsRenderer.renderKeyword(analysisSession, KtTokens.INIT_KEYWORD, symbol, this) },
                    { printer.withIndentInBraces {} },
                )
            }
        }
    }
}

