/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers

import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotated
import org.jetbrains.kotlin.lexer.KtKeywordToken

public interface KtRendererKeywordFilter {
    public fun filter(analysisSession: KtAnalysisSession, modifier: KtKeywordToken, ktAnnotated: KtAnnotated): Boolean

    public infix fun and(other: KtRendererKeywordFilter): KtRendererKeywordFilter {
        val self = this
        return KtRendererKeywordFilter filter@{ modifier, ktAnnotated ->
            val analysisSession = this@filter
            self.filter(analysisSession, modifier, ktAnnotated) && other.filter(analysisSession, modifier, ktAnnotated)
        }
    }

    public infix fun or(other: KtRendererKeywordFilter): KtRendererKeywordFilter {
        val self = this
        return KtRendererKeywordFilter filter@{ modifier, symbol ->
            val analysisSession = this@filter
            self.filter(analysisSession, modifier, symbol) || other.filter(analysisSession, modifier, symbol)
        }
    }

    public object ALL : KtRendererKeywordFilter {
        override fun filter(analysisSession: KtAnalysisSession, modifier: KtKeywordToken, ktAnnotated: KtAnnotated): Boolean {
            return true
        }
    }

    public object NONE : KtRendererKeywordFilter {
        override fun filter(analysisSession: KtAnalysisSession, modifier: KtKeywordToken, ktAnnotated: KtAnnotated): Boolean {
            return false
        }
    }

    public companion object {
        public operator fun invoke(
            predicate: KtAnalysisSession.(modifier: KtKeywordToken, ktAnnotated: KtAnnotated) -> Boolean
        ): KtRendererKeywordFilter =
            object : KtRendererKeywordFilter {
                override fun filter(analysisSession: KtAnalysisSession, modifier: KtKeywordToken, ktAnnotated: KtAnnotated): Boolean {
                    return predicate(analysisSession, modifier, ktAnnotated)
                }
            }

        public fun onlyWith(vararg modifiers: KtKeywordToken): KtRendererKeywordFilter =
            KtRendererKeywordFilter { modifier, _ -> modifier in modifiers }

        public fun onlyWith(modifiers: TokenSet): KtRendererKeywordFilter =
            KtRendererKeywordFilter { modifier, _ -> modifier in modifiers }

        public fun without(vararg modifiers: KtKeywordToken): KtRendererKeywordFilter =
            KtRendererKeywordFilter { modifier, _ -> modifier !in modifiers }

        public fun without(modifiers: TokenSet): KtRendererKeywordFilter =
            KtRendererKeywordFilter { modifier, _ -> modifier !in modifiers }
    }
}
