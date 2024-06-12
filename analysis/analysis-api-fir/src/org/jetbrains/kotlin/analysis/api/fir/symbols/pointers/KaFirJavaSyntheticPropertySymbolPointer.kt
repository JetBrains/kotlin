/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSyntheticJavaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticPropertiesScope
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.Name

internal class KaFirJavaSyntheticPropertySymbolPointer(
    ownerPointer: KaSymbolPointer<KaSymbolWithMembers>,
    private val propertyName: Name,
    private val isSynthetic: Boolean,
) : KaFirMemberSymbolPointer<KaSyntheticJavaPropertySymbol>(ownerPointer) {
    override fun KaFirSession.chooseCandidateAndCreateSymbol(
        candidates: FirScope,
        firSession: FirSession,
    ): KaSyntheticJavaPropertySymbol? {
        val syntheticProperty = candidates.getProperties(propertyName)
            .mapNotNull { it.fir as? FirSyntheticProperty }
            .singleOrNull()
            ?: return null

        return firSymbolBuilder.variableLikeBuilder.buildSyntheticJavaPropertySymbol(syntheticProperty.symbol)
    }

    override fun getSearchScope(analysisSession: KaFirSession, owner: FirClassSymbol<*>): FirScope? {
        val baseScope = super.getSearchScope(analysisSession, owner) as? FirTypeScope ?: return null
        return if (isSynthetic) {
            FirSyntheticPropertiesScope.createIfSyntheticNamesProviderIsDefined(
                session = analysisSession.firSession,
                dispatchReceiverType = owner.defaultType(),
                baseScope = baseScope,
            )
        } else {
            baseScope
        }
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirJavaSyntheticPropertySymbolPointer &&
            other.propertyName == propertyName &&
            other.isSynthetic == isSynthetic &&
            hasTheSameOwner(other)
}
