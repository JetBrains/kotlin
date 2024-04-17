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
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderClassInitializer(symbol: KtClassInitializerSymbol, printer: PrettyPrinter)

    public object INIT_BLOCK_WITH_BRACES : KtClassInitializerRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderClassInitializer(symbol: KtClassInitializerSymbol, printer: PrettyPrinter): Unit = printer {
            " ".separated(
                {
                    keywordsRenderer.renderKeyword(KtTokens.INIT_KEYWORD, symbol, this)
                },
                { printer.withIndentInBraces {} },
            )
        }
    }
}

