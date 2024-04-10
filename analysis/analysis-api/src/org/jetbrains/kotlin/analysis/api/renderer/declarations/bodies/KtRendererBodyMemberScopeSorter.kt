/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.bodies

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers

public interface KtRendererBodyMemberScopeSorter {
    public fun sortMembers(
        analysisSession: KtAnalysisSession,
        members: List<KtDeclarationSymbol>,
        owner: KtSymbolWithMembers,
    ): List<KtDeclarationSymbol>

    public object ENUM_ENTRIES_AT_BEGINING : KtRendererBodyMemberScopeSorter {
        override fun sortMembers(
            analysisSession: KtAnalysisSession,
            members: List<KtDeclarationSymbol>,
            owner: KtSymbolWithMembers,
        ): List<KtDeclarationSymbol> {
            return members.sortedBy { it !is KtEnumEntrySymbol }
        }
    }
}
