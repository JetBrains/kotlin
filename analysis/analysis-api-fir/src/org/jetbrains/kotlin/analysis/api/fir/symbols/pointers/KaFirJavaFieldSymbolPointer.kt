/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.symbols.KaJavaFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.name.Name

internal class KaFirJavaFieldSymbolPointer(
    ownerPointer: KaSymbolPointer<KaSymbolWithMembers>,
    private val fieldName: Name,
    isStatic: Boolean,
) : KaFirMemberSymbolPointer<KaJavaFieldSymbol>(ownerPointer, isStatic) {
    override fun KaFirSession.chooseCandidateAndCreateSymbol(
        candidates: FirScope,
        firSession: FirSession,
    ): KaJavaFieldSymbol? {
        val javaField = candidates.getProperties(fieldName).mapNotNull { it.fir as? FirJavaField }.singleOrNull() ?: return null
        return firSymbolBuilder.variableLikeBuilder.buildFieldSymbol(javaField.symbol)
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirJavaFieldSymbolPointer &&
            other.fieldName == fieldName &&
            hasTheSameOwner(other)
}
