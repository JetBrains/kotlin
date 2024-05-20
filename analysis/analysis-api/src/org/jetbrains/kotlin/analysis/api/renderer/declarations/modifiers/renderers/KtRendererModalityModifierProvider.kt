/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithModality
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens

public interface KaRendererModalityModifierProvider {
    public fun getModalityModifier(analysisSession: KaSession, symbol: KaSymbolWithModality): KtModifierKeywordToken?

    public fun onlyIf(
        condition: KaSession.(symbol: KaSymbolWithModality) -> Boolean
    ): KaRendererModalityModifierProvider {
        val self = this
        return object : KaRendererModalityModifierProvider {
            override fun getModalityModifier(analysisSession: KaSession, symbol: KaSymbolWithModality): KtModifierKeywordToken? =
                if (condition(analysisSession, symbol)) self.getModalityModifier(analysisSession, symbol)
                else null
        }
    }

    public object WITH_IMPLICIT_MODALITY : KaRendererModalityModifierProvider {
        override fun getModalityModifier(analysisSession: KaSession, symbol: KaSymbolWithModality): KtModifierKeywordToken? {
            if (symbol is KaPropertyAccessorSymbol) return null
            return when (symbol.modality) {
                Modality.SEALED -> KtTokens.SEALED_KEYWORD
                Modality.OPEN -> KtTokens.OPEN_KEYWORD
                Modality.ABSTRACT -> KtTokens.ABSTRACT_KEYWORD
                Modality.FINAL -> KtTokens.FINAL_KEYWORD
            }
        }
    }

    public object WITHOUT_IMPLICIT_MODALITY : KaRendererModalityModifierProvider {
        override fun getModalityModifier(analysisSession: KaSession, symbol: KaSymbolWithModality): KtModifierKeywordToken? {
            with(analysisSession) {
                when (symbol) {
                    is KaFunctionSymbol -> if (symbol.isOverride && symbol.modality != Modality.FINAL) return null
                    is KaPropertySymbol -> if (symbol.isOverride && symbol.modality != Modality.FINAL) return null
                }
                if ((symbol as? KaClassOrObjectSymbol)?.classKind == KaClassKind.INTERFACE) return null
                if ((symbol.getContainingSymbol() as? KaClassOrObjectSymbol)?.classKind == KaClassKind.INTERFACE) return null

                return when (symbol.modality) {
                    Modality.FINAL -> null
                    else -> WITH_IMPLICIT_MODALITY.getModalityModifier(analysisSession, symbol)
                }
            }
        }
    }
}

public typealias KtRendererModalityModifierProvider = KaRendererModalityModifierProvider