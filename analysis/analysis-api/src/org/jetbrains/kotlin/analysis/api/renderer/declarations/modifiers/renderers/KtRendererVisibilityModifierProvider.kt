/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens

public interface KtRendererVisibilityModifierProvider {
    context(KtAnalysisSession)
    public fun getVisibilityModifier(symbol: KtSymbolWithVisibility): KtModifierKeywordToken?

    public fun onlyIf(
        condition: context(KtAnalysisSession) (symbol: KtSymbolWithVisibility) -> Boolean
    ): KtRendererVisibilityModifierProvider {
        val self = this
        return object : KtRendererVisibilityModifierProvider {
            context(KtAnalysisSession)
            override fun getVisibilityModifier(symbol: KtSymbolWithVisibility): KtModifierKeywordToken? =
                if (condition(this@KtAnalysisSession, symbol)) self.getVisibilityModifier(symbol)
                else null
        }
    }

    public object NO_IMPLICIT_VISIBILITY : KtRendererVisibilityModifierProvider {
        context(KtAnalysisSession)
        override fun getVisibilityModifier(symbol: KtSymbolWithVisibility): KtModifierKeywordToken? {
            when (symbol) {
                is KtFunctionSymbol -> if (symbol.isOverride) return null
                is KtPropertySymbol -> if (symbol.isOverride) return null
                is KtConstructorSymbol -> {
                    if ((symbol.getContainingSymbol() as? KtClassOrObjectSymbol)?.classKind == KtClassKind.ENUM_CLASS) return null
                }
            }

            return when (symbol.visibility) {
                Visibilities.Public -> null
                JavaVisibilities.PackageVisibility -> null
                JavaVisibilities.ProtectedStaticVisibility, JavaVisibilities.ProtectedAndPackage -> null
                else -> WITH_IMPLICIT_VISIBILITY.getVisibilityModifier(symbol)
            }
        }
    }

    public object WITH_IMPLICIT_VISIBILITY : KtRendererVisibilityModifierProvider {
        context(KtAnalysisSession)
        override fun getVisibilityModifier(symbol: KtSymbolWithVisibility): KtModifierKeywordToken? {
            return when (symbol.visibility) {
                Visibilities.Private, Visibilities.PrivateToThis -> KtTokens.PRIVATE_KEYWORD
                Visibilities.Protected -> KtTokens.PROTECTED_KEYWORD
                Visibilities.Internal -> KtTokens.INTERNAL_KEYWORD
                Visibilities.Public -> KtTokens.PUBLIC_KEYWORD
                Visibilities.Local -> null
                JavaVisibilities.PackageVisibility -> KtTokens.PUBLIC_KEYWORD
                JavaVisibilities.ProtectedStaticVisibility, JavaVisibilities.ProtectedAndPackage -> KtTokens.PROTECTED_KEYWORD
                else -> null
            }
        }
    }
}
