/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.KaDeclarationModifiersRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithModality
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithVisibility
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken

@KaExperimentalApi
public interface KaModifierListRenderer {
    public fun renderModifiers(
        analysisSession: KaSession,
        symbol: KaDeclarationSymbol,
        declarationModifiersRenderer: KaDeclarationModifiersRenderer,
        printer: PrettyPrinter,
    )

    public object AS_LIST : KaModifierListRenderer {
        override fun renderModifiers(
            analysisSession: KaSession,
            symbol: KaDeclarationSymbol,
            declarationModifiersRenderer: KaDeclarationModifiersRenderer,
            printer: PrettyPrinter,
        ) {
            val modifiers = getModifiers(analysisSession, symbol, declarationModifiersRenderer)
                .distinct()
                .let { declarationModifiersRenderer.modifiersSorter.sort(analysisSession, it, symbol) }
                .ifEmpty { return }

            declarationModifiersRenderer.keywordsRenderer.renderKeywords(analysisSession, modifiers, symbol, printer)
        }

        private fun getModifiers(
            analysisSession: KaSession,
            symbol: KaDeclarationSymbol,
            declarationModifiersRenderer: KaDeclarationModifiersRenderer,
        ): List<KtModifierKeywordToken> {
            return buildList {
                if (symbol is KaSymbolWithVisibility) {
                    declarationModifiersRenderer.visibilityProvider.getVisibilityModifier(analysisSession, symbol)?.let(::add)
                }

                if (symbol is KaSymbolWithModality) {
                    declarationModifiersRenderer.modalityProvider.getModalityModifier(analysisSession, symbol)?.let(::add)
                }

                addAll(declarationModifiersRenderer.otherModifiersProvider.getOtherModifiers(analysisSession, symbol))
            }
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaModifierListRenderer' instead", ReplaceWith("KaModifierListRenderer"))
public typealias KtModifierListRenderer = KaModifierListRenderer