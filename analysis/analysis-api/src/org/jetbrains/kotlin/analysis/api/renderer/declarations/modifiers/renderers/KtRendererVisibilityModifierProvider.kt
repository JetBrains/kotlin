/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens

@KaExperimentalApi
public interface KaRendererVisibilityModifierProvider {
    public fun getVisibilityModifier(analysisSession: KaSession, symbol: KaSymbolWithVisibility): KtModifierKeywordToken?

    public fun onlyIf(
        condition: KaSession.(symbol: KaSymbolWithVisibility) -> Boolean
    ): KaRendererVisibilityModifierProvider {
        val self = this
        return object : KaRendererVisibilityModifierProvider {
            override fun getVisibilityModifier(analysisSession: KaSession, symbol: KaSymbolWithVisibility): KtModifierKeywordToken? =
                if (condition(analysisSession, symbol)) self.getVisibilityModifier(analysisSession, symbol) else null
        }
    }

    public object NO_IMPLICIT_VISIBILITY : KaRendererVisibilityModifierProvider {
        override fun getVisibilityModifier(
            analysisSession: KaSession,
            symbol: KaSymbolWithVisibility,
        ): KtModifierKeywordToken? {
            with(analysisSession) {
                when (symbol) {
                    is KaFunctionSymbol -> if (symbol.isOverride) return null
                    is KaPropertySymbol -> if (symbol.isOverride) return null
                    is KaConstructorSymbol -> {
                        if ((symbol.containingSymbol as? KaClassOrObjectSymbol)?.classKind == KaClassKind.ENUM_CLASS) return null
                    }
                }

                return when (symbol.visibility) {
                    Visibilities.Public -> null
                    JavaVisibilities.PackageVisibility -> null
                    JavaVisibilities.ProtectedStaticVisibility, JavaVisibilities.ProtectedAndPackage -> null
                    else -> WITH_IMPLICIT_VISIBILITY.getVisibilityModifier(analysisSession, symbol)
                }
            }
        }
    }

    public object WITH_IMPLICIT_VISIBILITY : KaRendererVisibilityModifierProvider {
        override fun getVisibilityModifier(
            analysisSession: KaSession,
            symbol: KaSymbolWithVisibility,
        ): KtModifierKeywordToken? {
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

@KaExperimentalApi
@Deprecated("Use 'KaRendererVisibilityModifierProvider' instead", ReplaceWith("KaRendererVisibilityModifierProvider"))
public typealias KtRendererVisibilityModifierProvider = KaRendererVisibilityModifierProvider