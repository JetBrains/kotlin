/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers

import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken

public interface KtRendererModifierFilter {
    context(KtAnalysisSession)
    public fun filter(modifier: KtModifierKeywordToken, symbol: KtDeclarationSymbol): Boolean

    public infix fun and(other: KtRendererModifierFilter): KtRendererModifierFilter {
        val self = this
        return KtRendererModifierFilter { modifier, symbol ->
            self.filter(modifier, symbol) && other.filter(modifier, symbol)
        }
    }

    public infix fun or(other: KtRendererModifierFilter): KtRendererModifierFilter {
        val self = this
        return KtRendererModifierFilter { modifier, symbol ->
            self.filter(modifier, symbol) || other.filter(modifier, symbol)
        }
    }

    public object ALL : KtRendererModifierFilter {
        context(KtAnalysisSession)
        override fun filter(modifier: KtModifierKeywordToken, symbol: KtDeclarationSymbol): Boolean {
            return true
        }
    }

    public object NONE : KtRendererModifierFilter {
        context(KtAnalysisSession)
        override fun filter(modifier: KtModifierKeywordToken, symbol: KtDeclarationSymbol): Boolean {
            return false
        }
    }

    public companion object {
        public operator fun invoke(
            predicate: context(KtAnalysisSession)(modifier: KtModifierKeywordToken, symbol: KtDeclarationSymbol) -> Boolean
        ): KtRendererModifierFilter =
            object : KtRendererModifierFilter {
                context(KtAnalysisSession)
                override fun filter(modifier: KtModifierKeywordToken, symbol: KtDeclarationSymbol): Boolean {
                    return predicate(this@KtAnalysisSession, modifier, symbol)
                }
            }

        public fun onlyWith(vararg modifiers: KtModifierKeywordToken): KtRendererModifierFilter =
            KtRendererModifierFilter { modifier, _ -> modifier in modifiers }

        public fun onlyWith(modifiers: TokenSet): KtRendererModifierFilter =
            KtRendererModifierFilter { modifier, _ -> modifier in modifiers }

        public fun without(vararg modifiers: KtModifierKeywordToken): KtRendererModifierFilter =
            KtRendererModifierFilter { modifier, _ -> modifier !in modifiers }

        public fun without(modifiers: TokenSet): KtRendererModifierFilter =
            KtRendererModifierFilter { modifier, _ -> modifier !in modifiers }
    }
}
