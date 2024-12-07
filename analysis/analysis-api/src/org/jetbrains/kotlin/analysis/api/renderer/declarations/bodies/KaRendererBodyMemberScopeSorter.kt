/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.bodies

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol

@KaExperimentalApi
public interface KaRendererBodyMemberScopeSorter {
    public fun sortMembers(
        analysisSession: KaSession,
        members: List<KaDeclarationSymbol>,
        container: KaDeclarationContainerSymbol,
    ): List<KaDeclarationSymbol>

    @KaExperimentalApi
    public object ENUM_ENTRIES_AT_BEGINING : KaRendererBodyMemberScopeSorter {
        override fun sortMembers(
            analysisSession: KaSession,
            members: List<KaDeclarationSymbol>,
            container: KaDeclarationContainerSymbol,
        ): List<KaDeclarationSymbol> {
            return members.sortedBy { it !is KaEnumEntrySymbol }
        }
    }
}
