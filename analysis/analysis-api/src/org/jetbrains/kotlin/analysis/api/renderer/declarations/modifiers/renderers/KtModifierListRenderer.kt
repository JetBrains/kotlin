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
    context(KtAnalysisSession, KtDeclarationModifiersRenderer)
    public fun renderModifiers(symbol: KtDeclarationSymbol, printer: PrettyPrinter)

    public object AS_LIST : KtModifierListRenderer {
        context(KtAnalysisSession, KtDeclarationModifiersRenderer)
        override fun renderModifiers(symbol: KtDeclarationSymbol, printer: PrettyPrinter) {
            val modifiers = getModifiers(symbol)
                .distinct()
                .let { modifiersSorter.sort(it, symbol) }
                .ifEmpty { return }
            keywordsRenderer.renderKeywords(modifiers, symbol, printer)
        }

        context(KtAnalysisSession, KtDeclarationModifiersRenderer)
        private fun getModifiers(symbol: KtDeclarationSymbol): List<KtModifierKeywordToken> {
            return buildList {
                if (symbol is KtSymbolWithVisibility) {
                    visibilityProvider.getVisibilityModifier(symbol)?.let(::add)
                }
                if (symbol is KtSymbolWithModality) {
                    modalityProvider.getModalityModifier(symbol)?.let(::add)
                }
                addAll(otherModifiersProvider.getOtherModifiers(symbol))
            }
        }
    }
}

