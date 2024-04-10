/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.bodies

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers

public interface KtRendererBodyMemberScopeProvider {
    public fun getMemberScope(analysisSession: KtAnalysisSession, symbol: KtSymbolWithMembers): List<KtDeclarationSymbol>

    public object ALL : KtRendererBodyMemberScopeProvider {
        override fun getMemberScope(analysisSession: KtAnalysisSession, symbol: KtSymbolWithMembers): List<KtDeclarationSymbol> {
            with(analysisSession) {
                return symbol.getCombinedDeclaredMemberScope().getAllSymbols().toList()
            }
        }
    }

    public object ALL_DECLARED : KtRendererBodyMemberScopeProvider {
        override fun getMemberScope(analysisSession: KtAnalysisSession, symbol: KtSymbolWithMembers): List<KtDeclarationSymbol> {
            with(analysisSession) {
                return symbol.getCombinedDeclaredMemberScope().getAllSymbols()
                    .filter { member ->
                        val origin = member.origin
                        origin != KtSymbolOrigin.DELEGATED &&
                                origin != KtSymbolOrigin.SOURCE_MEMBER_GENERATED &&
                                origin != KtSymbolOrigin.SUBSTITUTION_OVERRIDE &&
                                origin != KtSymbolOrigin.INTERSECTION_OVERRIDE
                    }.filter { member ->
                        member !is KtConstructorSymbol || symbol !is KtClassOrObjectSymbol || !symbol.classKind.isObject
                    }.filterNot { member ->
                        member is KtConstructorSymbol && symbol is KtEnumEntrySymbol
                    }
                    .toList()
            }
        }
    }

    public object NONE : KtRendererBodyMemberScopeProvider {
        override fun getMemberScope(analysisSession: KtAnalysisSession, symbol: KtSymbolWithMembers): List<KtDeclarationSymbol> {
            return emptyList()
        }
    }
}
