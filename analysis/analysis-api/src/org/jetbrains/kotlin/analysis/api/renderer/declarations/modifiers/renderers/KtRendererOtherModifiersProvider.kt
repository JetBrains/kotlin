/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaPossibleMultiplatformSymbol
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.types.Variance

public interface KaRendererOtherModifiersProvider {
    public fun getOtherModifiers(analysisSession: KaSession, symbol: KaDeclarationSymbol): List<KtModifierKeywordToken>

    public infix fun and(other: KaRendererOtherModifiersProvider): KaRendererOtherModifiersProvider {
        val self = this
        return object : KaRendererOtherModifiersProvider {
            override fun getOtherModifiers(analysisSession: KaSession, symbol: KaDeclarationSymbol): List<KtModifierKeywordToken> {
                return self.getOtherModifiers(analysisSession, symbol) + other.getOtherModifiers(analysisSession, symbol)
            }
        }
    }

    public fun onlyIf(
        condition: KaSession.(symbol: KaDeclarationSymbol) -> Boolean
    ): KaRendererOtherModifiersProvider {
        val self = this
        return object : KaRendererOtherModifiersProvider {
            override fun getOtherModifiers(analysisSession: KaSession, symbol: KaDeclarationSymbol): List<KtModifierKeywordToken> {
                return if (condition(analysisSession, symbol)) {
                    self.getOtherModifiers(analysisSession, symbol)
                } else {
                    emptyList()
                }
            }
        }
    }

    public object ALL : KaRendererOtherModifiersProvider {
        override fun getOtherModifiers(analysisSession: KaSession, symbol: KaDeclarationSymbol): List<KtModifierKeywordToken> {
            return buildList {
                if (symbol is KaPossibleMultiplatformSymbol) {
                    if (symbol.isActual) add(KtTokens.ACTUAL_KEYWORD)
                    if (symbol.isExpect) add(KtTokens.EXPECT_KEYWORD)
                }

                if (symbol is KaFunctionSymbol) {
                    if (symbol.isExternal) add(KtTokens.EXTERNAL_KEYWORD)
                    if (symbol.isOverride) add(KtTokens.OVERRIDE_KEYWORD)
                    if (symbol.isInline) add(KtTokens.INLINE_KEYWORD)
                    if (symbol.isInfix) add(KtTokens.INFIX_KEYWORD)
                    if (symbol.isOperator) add(KtTokens.OPERATOR_KEYWORD)
                    if (symbol.isSuspend) add(KtTokens.SUSPEND_KEYWORD)
                    if (symbol.isTailRec) add(KtTokens.TAILREC_KEYWORD)
                }

                if (symbol is KaPropertySymbol) {
                    if (symbol.isOverride) add(KtTokens.OVERRIDE_KEYWORD)
                }

                if (symbol is KaValueParameterSymbol) {
                    if (symbol.isVararg) add(KtTokens.VARARG_KEYWORD)
                    if (symbol.isCrossinline) add(KtTokens.CROSSINLINE_KEYWORD)
                    if (symbol.isNoinline) add(KtTokens.NOINLINE_KEYWORD)
                }

                if (symbol is KaKotlinPropertySymbol) {
                    if (symbol.isConst) add(KtTokens.CONST_KEYWORD)
                    if (symbol.isLateInit) add(KtTokens.LATEINIT_KEYWORD)
                }

                if (symbol is KaNamedClassOrObjectSymbol) {
                    if (symbol.isExternal) add(KtTokens.EXTERNAL_KEYWORD)
                    if (symbol.isInline) add(KtTokens.INLINE_KEYWORD)
                    if (symbol.isData) add(KtTokens.DATA_KEYWORD)
                    if (symbol.isFun) add(KtTokens.FUN_KEYWORD)
                    if (symbol.isInner) add(KtTokens.INNER_KEYWORD)
                }

                if (symbol is KaTypeParameterSymbol) {
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
}

public typealias KtRendererOtherModifiersProvider = KaRendererOtherModifiersProvider