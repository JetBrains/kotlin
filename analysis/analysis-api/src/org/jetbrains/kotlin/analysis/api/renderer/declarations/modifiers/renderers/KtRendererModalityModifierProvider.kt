/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithModality
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens

public interface KtRendererModalityModifierProvider {
    context(KtAnalysisSession)
    public fun getModalityModifier(symbol: KtSymbolWithModality): KtModifierKeywordToken?

    public fun onlyIf(
        condition: context(KtAnalysisSession) (symbol: KtSymbolWithModality) -> Boolean
    ): KtRendererModalityModifierProvider {
        val self = this
        return object : KtRendererModalityModifierProvider {
            context(KtAnalysisSession)
            override fun getModalityModifier(symbol: KtSymbolWithModality): KtModifierKeywordToken? =
                if (condition(this@KtAnalysisSession, symbol)) self.getModalityModifier(symbol)
                else null
        }
    }

    public object WITH_IMPLICIT_MODALITY : KtRendererModalityModifierProvider {
        context(KtAnalysisSession)
        override fun getModalityModifier(symbol: KtSymbolWithModality): KtModifierKeywordToken? {
            if (symbol is KtPropertyAccessorSymbol) return null
            return when (symbol.modality) {
                Modality.SEALED -> KtTokens.SEALED_KEYWORD
                Modality.OPEN -> KtTokens.OPEN_KEYWORD
                Modality.ABSTRACT -> KtTokens.ABSTRACT_KEYWORD
                Modality.FINAL -> KtTokens.FINAL_KEYWORD
            }
        }
    }

    public object WITHOUT_IMPLICIT_MODALITY : KtRendererModalityModifierProvider {
        context(KtAnalysisSession) override fun getModalityModifier(symbol: KtSymbolWithModality): KtModifierKeywordToken? {
            when (symbol) {
                is KtFunctionSymbol -> if (symbol.isOverride && symbol.modality != Modality.FINAL) return null
                is KtPropertySymbol -> if (symbol.isOverride && symbol.modality != Modality.FINAL) return null
            }
            if ((symbol as? KtClassOrObjectSymbol)?.classKind == KtClassKind.INTERFACE) return null
            if ((symbol.getContainingSymbol() as? KtClassOrObjectSymbol)?.classKind == KtClassKind.INTERFACE) return null

            return when (symbol.modality) {
                Modality.FINAL -> null
                else -> WITH_IMPLICIT_MODALITY.getModalityModifier(symbol)
            }
        }
    }
}
