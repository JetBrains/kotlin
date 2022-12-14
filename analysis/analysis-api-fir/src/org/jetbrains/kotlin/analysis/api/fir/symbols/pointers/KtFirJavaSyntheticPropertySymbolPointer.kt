/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtSyntheticJavaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticPropertiesScope
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.Name

internal class KtFirJavaSyntheticPropertySymbolPointer(
    ownerPointer: KtSymbolPointer<KtSymbolWithMembers>,
    private val propertyName: Name,
    private val isSynthetic: Boolean,
) : KtFirMemberSymbolPointer<KtSyntheticJavaPropertySymbol>(ownerPointer) {
    override fun KtFirAnalysisSession.chooseCandidateAndCreateSymbol(
        candidates: FirScope,
        firSession: FirSession,
    ): KtSyntheticJavaPropertySymbol? {
        val syntheticProperty = candidates.getProperties(propertyName)
            .mapNotNull { it.fir as? FirSyntheticProperty }
            .singleOrNull()
            ?: return null

        return firSymbolBuilder.variableLikeBuilder.buildSyntheticJavaPropertySymbol(syntheticProperty.symbol)
    }

    context(KtFirAnalysisSession)
    override fun getSearchScope(owner: FirClassSymbol<*>): FirScope? {
        val baseScope = super.getSearchScope(owner) as? FirTypeScope ?: return null
        return if (isSynthetic) {
            FirSyntheticPropertiesScope.createIfSyntheticNamesProviderIsDefined(
                session = useSiteSession,
                dispatchReceiverType = owner.defaultType(),
                baseScope = baseScope,
            )
        } else {
            baseScope
        }
    }
}
