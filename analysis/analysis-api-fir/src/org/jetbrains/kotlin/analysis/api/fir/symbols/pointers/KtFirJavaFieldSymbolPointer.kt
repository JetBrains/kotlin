/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtJavaFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.name.Name

internal class KtFirJavaFieldSymbolPointer(
    ownerPointer: KtSymbolPointer<KtSymbolWithMembers>,
    private val fieldName: Name,
    isStatic: Boolean,
) : KtFirMemberSymbolPointer<KtJavaFieldSymbol>(ownerPointer, isStatic) {
    override fun KtFirAnalysisSession.chooseCandidateAndCreateSymbol(
        candidates: FirScope,
        firSession: FirSession,
    ): KtJavaFieldSymbol? {
        val javaField = candidates.getProperties(fieldName).mapNotNull { it.fir as? FirJavaField }.singleOrNull() ?: return null
        return firSymbolBuilder.variableLikeBuilder.buildFieldSymbol(javaField.symbol)
    }

    override fun pointsToTheSameSymbolAs(other: KtSymbolPointer<KtSymbol>): Boolean = this === other ||
            other is KtFirJavaFieldSymbolPointer &&
            other.fieldName == fieldName &&
            hasTheSameOwner(other)
}
