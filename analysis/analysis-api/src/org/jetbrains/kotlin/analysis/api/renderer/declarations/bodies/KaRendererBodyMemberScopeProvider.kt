/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.bodies

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol

@KaExperimentalApi
public interface KaRendererBodyMemberScopeProvider {
    public fun getMemberScope(analysisSession: KaSession, symbol: KaDeclarationContainerSymbol): List<KaDeclarationSymbol>

    @KaExperimentalApi
    public object ALL : KaRendererBodyMemberScopeProvider {
        override fun getMemberScope(analysisSession: KaSession, symbol: KaDeclarationContainerSymbol): List<KaDeclarationSymbol> {
            with(analysisSession) {
                return symbol.combinedDeclaredMemberScope.declarations.toList()
            }
        }
    }

    @KaExperimentalApi
    public object ALL_DECLARED : KaRendererBodyMemberScopeProvider {
        override fun getMemberScope(analysisSession: KaSession, symbol: KaDeclarationContainerSymbol): List<KaDeclarationSymbol> {
            with(analysisSession) {
                return symbol.combinedDeclaredMemberScope.declarations
                    .filter { member ->
                        val origin = member.origin
                        origin != KaSymbolOrigin.DELEGATED &&
                                origin != KaSymbolOrigin.SOURCE_MEMBER_GENERATED &&
                                origin != KaSymbolOrigin.SUBSTITUTION_OVERRIDE &&
                                origin != KaSymbolOrigin.INTERSECTION_OVERRIDE
                    }.filter { member ->
                        member !is KaConstructorSymbol || symbol !is KaClassSymbol || !symbol.classKind.isObject
                    }.filterNot { member ->
                        member is KaConstructorSymbol && symbol is KaEnumEntrySymbol
                    }
                    .toList()
            }
        }
    }

    @KaExperimentalApi
    public object NONE : KaRendererBodyMemberScopeProvider {
        override fun getMemberScope(analysisSession: KaSession, symbol: KaDeclarationContainerSymbol): List<KaDeclarationSymbol> {
            return emptyList()
        }
    }
}
