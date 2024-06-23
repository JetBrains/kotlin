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
public interface KaRendererModalityModifierProvider {
    public fun getModalityModifier(analysisSession: KaSession, symbol: KaDeclarationSymbol): KtModifierKeywordToken?

    public fun onlyIf(
        condition: KaSession.(symbol: KaDeclarationSymbol) -> Boolean
    ): KaRendererModalityModifierProvider {
        val self = this
        return object : KaRendererModalityModifierProvider {
            override fun getModalityModifier(analysisSession: KaSession, symbol: KaDeclarationSymbol): KtModifierKeywordToken? =
                if (condition(analysisSession, symbol)) self.getModalityModifier(analysisSession, symbol)
                else null
        }
    }

    public object WITH_IMPLICIT_MODALITY : KaRendererModalityModifierProvider {
        override fun getModalityModifier(analysisSession: KaSession, symbol: KaDeclarationSymbol): KtModifierKeywordToken? = when (symbol) {
            is KaPropertyAccessorSymbol,
            is KaValueParameterSymbol,
            is KaBackingFieldSymbol,
            is KaScriptSymbol,
            is KaClassInitializerSymbol,
            is KaTypeParameterSymbol,
            is KaDestructuringDeclarationSymbol,
            is KaConstructorSymbol,
            is KaEnumEntrySymbol,
            is KaTypeAliasSymbol,
            is KaAnonymousFunctionSymbol,
            is KaAnonymousObjectSymbol,
            is KaSamConstructorSymbol,
            is KaLocalVariableSymbol,
                -> null

            else -> when (symbol.modality) {
                KaSymbolModality.FINAL -> KtTokens.FINAL_KEYWORD
                KaSymbolModality.SEALED -> KtTokens.SEALED_KEYWORD
                KaSymbolModality.OPEN -> KtTokens.OPEN_KEYWORD
                KaSymbolModality.ABSTRACT -> KtTokens.ABSTRACT_KEYWORD
            }
        }
    }

    public object WITHOUT_IMPLICIT_MODALITY : KaRendererModalityModifierProvider {
        override fun getModalityModifier(analysisSession: KaSession, symbol: KaDeclarationSymbol): KtModifierKeywordToken? {
            with(analysisSession) {
                when (symbol) {
                    is KaNamedFunctionSymbol -> if (symbol.isOverride && symbol.modality != KaSymbolModality.FINAL) return null
                    is KaPropertySymbol -> if (symbol.isOverride && symbol.modality != KaSymbolModality.FINAL) return null
                    is KaClassSymbol -> if (symbol.classKind == KaClassKind.INTERFACE) return null
                    else -> {}
                }

                if (symbol.location == KaSymbolLocation.CLASS) {
                    if ((symbol.containingSymbol as? KaClassSymbol)?.classKind == KaClassKind.INTERFACE) return null
                }

                return when (symbol.modality) {
                    KaSymbolModality.FINAL -> null
                    else -> WITH_IMPLICIT_MODALITY.getModalityModifier(analysisSession, symbol)
                }
            }
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaRendererModalityModifierProvider' instead", ReplaceWith("KaRendererModalityModifierProvider"))
public typealias KtRendererModalityModifierProvider = KaRendererModalityModifierProvider