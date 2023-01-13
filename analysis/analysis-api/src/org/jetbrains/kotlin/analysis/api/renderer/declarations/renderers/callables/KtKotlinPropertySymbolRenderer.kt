/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens

public interface KtKotlinPropertySymbolRenderer {
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderSymbol(symbol: KtKotlinPropertySymbol, printer: PrettyPrinter)

    public object AS_SOURCE : KtKotlinPropertySymbolRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSymbol(symbol: KtKotlinPropertySymbol, printer: PrettyPrinter): Unit = printer {
            val mutabilityKeyword = if (symbol.isVal) KtTokens.VAL_KEYWORD else KtTokens.VAR_KEYWORD
            callableSignatureRenderer.renderCallableSignature(symbol, mutabilityKeyword, printer)
            variableInitializerRenderer.renderInitializer(symbol, printer)
            propertyAccessorsRenderer.renderAccessors(symbol, printer)
        }
    }
}