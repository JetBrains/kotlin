/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens

@KaExperimentalApi
public interface KaRendererVisibilityModifierProvider {
    public fun getVisibilityModifier(analysisSession: KaSession, symbol: KaDeclarationSymbol): KtModifierKeywordToken?

    public fun onlyIf(
        condition: KaSession.(symbol: KaDeclarationSymbol) -> Boolean
    ): KaRendererVisibilityModifierProvider {
        val self = this
        return object : KaRendererVisibilityModifierProvider {
            override fun getVisibilityModifier(analysisSession: KaSession, symbol: KaDeclarationSymbol): KtModifierKeywordToken? =
                if (condition(analysisSession, symbol)) self.getVisibilityModifier(analysisSession, symbol) else null
        }
    }

    public object NO_IMPLICIT_VISIBILITY : KaRendererVisibilityModifierProvider {
        override fun getVisibilityModifier(
            analysisSession: KaSession,
            symbol: KaDeclarationSymbol,
        ): KtModifierKeywordToken? {
            with(analysisSession) {
                when (symbol) {
                    is KaNamedFunctionSymbol -> if (symbol.isOverride) return null
                    is KaPropertySymbol -> if (symbol.isOverride) return null
                    is KaConstructorSymbol -> {
                        if ((symbol.containingSymbol as? KaClassSymbol)?.classKind == KaClassKind.ENUM_CLASS) return null
                    }
                    else -> {}
                }

                return when (symbol.visibility) {
                    KaSymbolVisibility.PUBLIC,
                    KaSymbolVisibility.PACKAGE_PRIVATE,
                    KaSymbolVisibility.PACKAGE_PROTECTED,
                        -> null

                    else -> WITH_IMPLICIT_VISIBILITY.getVisibilityModifier(analysisSession, symbol)
                }
            }
        }
    }

    public object WITH_IMPLICIT_VISIBILITY : KaRendererVisibilityModifierProvider {
        override fun getVisibilityModifier(
            analysisSession: KaSession,
            symbol: KaDeclarationSymbol,
        ): KtModifierKeywordToken? = when (symbol) {
            is KaClassInitializerSymbol,
            is KaTypeParameterSymbol,
            is KaScriptSymbol,
            is KaValueParameterSymbol,
            is KaAnonymousFunctionSymbol,
            is KaAnonymousObjectSymbol,
                -> null

            else -> when (symbol.visibility) {
                KaSymbolVisibility.PRIVATE -> KtTokens.PRIVATE_KEYWORD
                KaSymbolVisibility.PROTECTED -> KtTokens.PROTECTED_KEYWORD
                KaSymbolVisibility.INTERNAL -> KtTokens.INTERNAL_KEYWORD
                KaSymbolVisibility.PUBLIC -> KtTokens.PUBLIC_KEYWORD
                KaSymbolVisibility.LOCAL -> null
                KaSymbolVisibility.PACKAGE_PRIVATE -> KtTokens.PUBLIC_KEYWORD
                KaSymbolVisibility.PACKAGE_PROTECTED -> KtTokens.PROTECTED_KEYWORD
                else -> null
            }
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaRendererVisibilityModifierProvider' instead", ReplaceWith("KaRendererVisibilityModifierProvider"))
public typealias KtRendererVisibilityModifierProvider = KaRendererVisibilityModifierProvider