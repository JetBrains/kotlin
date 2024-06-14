/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.bodies

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithMembers

@KaExperimentalApi
public interface KaRendererBodyMemberScopeSorter {
    public fun sortMembers(
        analysisSession: KaSession,
        members: List<KaDeclarationSymbol>,
        owner: KaSymbolWithMembers,
    ): List<KaDeclarationSymbol>

    public object ENUM_ENTRIES_AT_BEGINING : KaRendererBodyMemberScopeSorter {
        override fun sortMembers(
            analysisSession: KaSession,
            members: List<KaDeclarationSymbol>,
            owner: KaSymbolWithMembers,
        ): List<KaDeclarationSymbol> {
            return members.sortedBy { it !is KaEnumEntrySymbol }
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaRendererBodyMemberScopeSorter' instead", ReplaceWith("KaRendererBodyMemberScopeSorter"))
public typealias KtRendererBodyMemberScopeSorter = KaRendererBodyMemberScopeSorter