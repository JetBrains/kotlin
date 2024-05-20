/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers

import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.lexer.KtKeywordToken

public interface KaRendererKeywordFilter {
    public fun filter(analysisSession: KaSession, modifier: KtKeywordToken, annotated: KaAnnotated): Boolean

    public infix fun and(other: KaRendererKeywordFilter): KaRendererKeywordFilter {
        val self = this
        return KaRendererKeywordFilter filter@{ modifier, kaAnnotated ->
            val analysisSession = this@filter
            self.filter(analysisSession, modifier, kaAnnotated) && other.filter(analysisSession, modifier, kaAnnotated)
        }
    }

    public infix fun or(other: KaRendererKeywordFilter): KaRendererKeywordFilter {
        val self = this
        return KaRendererKeywordFilter filter@{ modifier, symbol ->
            val analysisSession = this@filter
            self.filter(analysisSession, modifier, symbol) || other.filter(analysisSession, modifier, symbol)
        }
    }

    public object ALL : KaRendererKeywordFilter {
        override fun filter(analysisSession: KaSession, modifier: KtKeywordToken, annotated: KaAnnotated): Boolean {
            return true
        }
    }

    public object NONE : KaRendererKeywordFilter {
        override fun filter(analysisSession: KaSession, modifier: KtKeywordToken, annotated: KaAnnotated): Boolean {
            return false
        }
    }

    public companion object {
        public operator fun invoke(
            predicate: KaSession.(modifier: KtKeywordToken, annotated: KaAnnotated) -> Boolean
        ): KaRendererKeywordFilter =
            object : KaRendererKeywordFilter {
                override fun filter(analysisSession: KaSession, modifier: KtKeywordToken, annotated: KaAnnotated): Boolean {
                    return predicate(analysisSession, modifier, annotated)
                }
            }

        public fun onlyWith(vararg modifiers: KtKeywordToken): KaRendererKeywordFilter =
            KaRendererKeywordFilter { modifier, _ -> modifier in modifiers }

        public fun onlyWith(modifiers: TokenSet): KaRendererKeywordFilter =
            KaRendererKeywordFilter { modifier, _ -> modifier in modifiers }

        public fun without(vararg modifiers: KtKeywordToken): KaRendererKeywordFilter =
            KaRendererKeywordFilter { modifier, _ -> modifier !in modifiers }

        public fun without(modifiers: TokenSet): KaRendererKeywordFilter =
            KaRendererKeywordFilter { modifier, _ -> modifier !in modifiers }
    }
}

public typealias KtRendererKeywordFilter = KaRendererKeywordFilter