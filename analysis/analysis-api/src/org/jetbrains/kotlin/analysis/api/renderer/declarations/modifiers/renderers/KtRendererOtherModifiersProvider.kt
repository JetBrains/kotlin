/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.types.Variance

public interface KtRendererOtherModifiersProvider {
    context(KtAnalysisSession)
    public fun getOtherModifiers(symbol: KtDeclarationSymbol): List<KtModifierKeywordToken>

    public infix fun and(other: KtRendererOtherModifiersProvider): KtRendererOtherModifiersProvider {
        val self = this
        return object : KtRendererOtherModifiersProvider {
            context(KtAnalysisSession)
            override fun getOtherModifiers(symbol: KtDeclarationSymbol): List<KtModifierKeywordToken> {
                return self.getOtherModifiers(symbol) + other.getOtherModifiers(symbol)
            }
        }
    }


    public fun onlyIf(
        condition: context(KtAnalysisSession) (symbol: KtDeclarationSymbol) -> Boolean
    ): KtRendererOtherModifiersProvider {
        val self = this
        return object : KtRendererOtherModifiersProvider {
            context(KtAnalysisSession)
            override fun getOtherModifiers(symbol: KtDeclarationSymbol): List<KtModifierKeywordToken> =
                if (condition(this@KtAnalysisSession, symbol)) self.getOtherModifiers(symbol)
                else emptyList()
        }
    }


    public object ALL : KtRendererOtherModifiersProvider {
        context(KtAnalysisSession)
        override fun getOtherModifiers(symbol: KtDeclarationSymbol): List<KtModifierKeywordToken> = buildList {
            if (symbol is KtFunctionSymbol) {
                if (symbol.isExternal) add(KtTokens.EXTERNAL_KEYWORD)
                if (symbol.isOverride) add(KtTokens.OVERRIDE_KEYWORD)
                if (symbol.isInline) add(KtTokens.INLINE_KEYWORD)
                if (symbol.isInfix) add(KtTokens.INFIX_KEYWORD)
                if (symbol.isOperator) add(KtTokens.OPERATOR_KEYWORD)
                if (symbol.isSuspend) add(KtTokens.SUSPEND_KEYWORD)
            }

            if (symbol is KtPropertySymbol) {
                if (symbol.isOverride) add(KtTokens.OVERRIDE_KEYWORD)
            }

            if (symbol is KtValueParameterSymbol) {
                if (symbol.isVararg) add(KtTokens.VARARG_KEYWORD)
                if (symbol.isCrossinline) add(KtTokens.CROSSINLINE_KEYWORD)
                if (symbol.isNoinline) add(KtTokens.NOINLINE_KEYWORD)
            }

            if (symbol is KtKotlinPropertySymbol) {
                if (symbol.isConst) add(KtTokens.CONST_KEYWORD)
                if (symbol.isLateInit) add(KtTokens.LATEINIT_KEYWORD)
            }

            if (symbol is KtNamedClassOrObjectSymbol) {
                if (symbol.isExternal) add(KtTokens.EXTERNAL_KEYWORD)
                if (symbol.isInline) add(KtTokens.INLINE_KEYWORD)
                if (symbol.isData) add(KtTokens.DATA_KEYWORD)
                if (symbol.isFun) add(KtTokens.FUN_KEYWORD)
                if (symbol.isInner) add(KtTokens.INNER_KEYWORD)
            }

            if (symbol is KtTypeParameterSymbol) {
                if (symbol.isReified) add(KtTokens.REIFIED_KEYWORD)
                when (symbol.variance) {
                    Variance.INVARIANT -> {}
                    Variance.IN_VARIANCE -> add(KtTokens.IN_KEYWORD)
                    Variance.OUT_VARIANCE -> add(KtTokens.OUT_KEYWORD)
                }
            }
        }
    }
}
