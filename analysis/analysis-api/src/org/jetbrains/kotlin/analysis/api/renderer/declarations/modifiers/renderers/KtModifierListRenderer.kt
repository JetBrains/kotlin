/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.KtDeclarationModifiersRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithModality
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken

public interface KtModifierListRenderer {
    public fun renderModifiers(
        analysisSession: KtAnalysisSession,
        symbol: KtDeclarationSymbol,
        declarationModifiersRenderer: KtDeclarationModifiersRenderer,
        printer: PrettyPrinter,
    )

    public object AS_LIST : KtModifierListRenderer {
        override fun renderModifiers(
            analysisSession: KtAnalysisSession,
            symbol: KtDeclarationSymbol,
            declarationModifiersRenderer: KtDeclarationModifiersRenderer,
            printer: PrettyPrinter,
        ) {
            val modifiers = getModifiers(analysisSession, symbol, declarationModifiersRenderer)
                .distinct()
                .let { declarationModifiersRenderer.modifiersSorter.sort(analysisSession, it, symbol) }
                .ifEmpty { return }

            declarationModifiersRenderer.keywordsRenderer.renderKeywords(analysisSession, modifiers, symbol, printer)
        }

        private fun getModifiers(
            analysisSession: KtAnalysisSession,
            symbol: KtDeclarationSymbol,
            declarationModifiersRenderer: KtDeclarationModifiersRenderer,
        ): List<KtModifierKeywordToken> {
            return buildList {
                if (symbol is KtSymbolWithVisibility) {
                    declarationModifiersRenderer.visibilityProvider.getVisibilityModifier(analysisSession, symbol)?.let(::add)
                }

                if (symbol is KtSymbolWithModality) {
                    declarationModifiersRenderer.modalityProvider.getModalityModifier(analysisSession, symbol)?.let(::add)
                }

                addAll(declarationModifiersRenderer.otherModifiersProvider.getOtherModifiers(analysisSession, symbol))
            }
        }
    }
}

