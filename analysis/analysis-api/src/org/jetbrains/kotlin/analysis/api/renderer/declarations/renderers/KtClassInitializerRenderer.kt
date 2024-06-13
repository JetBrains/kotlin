/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaClassInitializerSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens

public interface KaClassInitializerRenderer {
    public fun renderClassInitializer(
        analysisSession: KaSession,
        symbol: KaClassInitializerSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object INIT_BLOCK_WITH_BRACES : KaClassInitializerRenderer {
        override fun renderClassInitializer(
            analysisSession: KaSession,
            symbol: KaClassInitializerSymbol,
            declarationRenderer: KaDeclarationRenderer,
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

public typealias KtClassInitializerRenderer = KaClassInitializerRenderer