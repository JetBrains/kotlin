/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.bodies

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithMembers

public interface KaRendererBodyMemberScopeProvider {
    public fun getMemberScope(analysisSession: KaSession, symbol: KaSymbolWithMembers): List<KaDeclarationSymbol>

    public object ALL : KaRendererBodyMemberScopeProvider {
        override fun getMemberScope(analysisSession: KaSession, symbol: KaSymbolWithMembers): List<KaDeclarationSymbol> {
            with(analysisSession) {
                return symbol.getCombinedDeclaredMemberScope().getAllSymbols().toList()
            }
        }
    }

    public object ALL_DECLARED : KaRendererBodyMemberScopeProvider {
        override fun getMemberScope(analysisSession: KaSession, symbol: KaSymbolWithMembers): List<KaDeclarationSymbol> {
            with(analysisSession) {
                return symbol.getCombinedDeclaredMemberScope().getAllSymbols()
                    .filter { member ->
                        val origin = member.origin
                        origin != KaSymbolOrigin.DELEGATED &&
                                origin != KaSymbolOrigin.SOURCE_MEMBER_GENERATED &&
                                origin != KaSymbolOrigin.SUBSTITUTION_OVERRIDE &&
                                origin != KaSymbolOrigin.INTERSECTION_OVERRIDE
                    }.filter { member ->
                        member !is KaConstructorSymbol || symbol !is KaClassOrObjectSymbol || !symbol.classKind.isObject
                    }.filterNot { member ->
                        member is KaConstructorSymbol && symbol is KaEnumEntrySymbol
                    }
                    .toList()
            }
        }
    }

    public object NONE : KaRendererBodyMemberScopeProvider {
        override fun getMemberScope(analysisSession: KaSession, symbol: KaSymbolWithMembers): List<KaDeclarationSymbol> {
            return emptyList()
        }
    }
}

public typealias KtRendererBodyMemberScopeProvider = KaRendererBodyMemberScopeProvider