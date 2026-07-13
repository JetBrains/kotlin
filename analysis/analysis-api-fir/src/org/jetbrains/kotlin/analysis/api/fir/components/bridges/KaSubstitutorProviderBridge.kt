/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaSubstitutorProvider
import org.jetbrains.kotlin.analysis.api.components.KaUnificationSubstitutorPolicy
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaUnificationSubstitutorPolicy as KaEndpointUnificationSubstitutorPolicy
import org.jetbrains.kotlin.analysis.api.types.createInheritanceTypeSubstitutor as createInheritanceTypeSubstitutorEndpoint
import org.jetbrains.kotlin.analysis.api.types.createSubstitutor as createSubstitutorEndpoint
import org.jetbrains.kotlin.analysis.api.types.createSubtypingUnificationSubstitutor as createSubtypingUnificationSubstitutorEndpoint

@OptIn(KaExperimentalApi::class, KaIdeApi::class)
internal class KaSubstitutorProviderBridge(
    override val analysisSessionProvider: () -> KaSession,
) : KaBaseSessionComponent<KaSession>(), KaSubstitutorProvider {
    override fun createSubstitutor(mappings: Map<KaTypeParameterSymbol, KaType>): KaSubstitutor =
        context(analysisSession) { createSubstitutorEndpoint(mappings) }

    override fun createInheritanceTypeSubstitutor(subClass: KaClassSymbol, superClass: KaClassSymbol): KaSubstitutor? =
        context(analysisSession) { createInheritanceTypeSubstitutorEndpoint(subClass, superClass) }

    override fun createSubtypingUnificationSubstitutor(
        leftTypesToRightTypes: List<Pair<KaType, KaType>>,
        isFreeTypeParameter: (KaTypeParameterSymbol) -> Boolean,
    ): KaSubstitutor? =
        context(analysisSession) { createSubtypingUnificationSubstitutorEndpoint(leftTypesToRightTypes, isFreeTypeParameter) }

    override fun createSubtypingUnificationSubstitutor(
        leftType: KaType,
        rightType: KaType,
        constructionPolicy: KaUnificationSubstitutorPolicy,
    ): KaSubstitutor? =
        context(analysisSession) {
            createSubtypingUnificationSubstitutorEndpoint(
                leftType,
                rightType,
                constructionPolicy.toEndpointPolicy()
            )
        }

    override fun createSubtypingUnificationSubstitutor(
        leftTypesToRightTypes: List<Pair<KaType, KaType>>,
        constructionPolicy: KaUnificationSubstitutorPolicy,
    ): KaSubstitutor? =
        context(analysisSession) {
            createSubtypingUnificationSubstitutorEndpoint(
                leftTypesToRightTypes,
                constructionPolicy.toEndpointPolicy()
            )
        }
}

private fun KaUnificationSubstitutorPolicy.toEndpointPolicy(): KaEndpointUnificationSubstitutorPolicy = when (this) {
    KaUnificationSubstitutorPolicy.ASSIGN_LEFT -> KaEndpointUnificationSubstitutorPolicy.ASSIGN_LEFT
    KaUnificationSubstitutorPolicy.ASSIGN_RIGHT -> KaEndpointUnificationSubstitutorPolicy.ASSIGN_RIGHT
    KaUnificationSubstitutorPolicy.ASSIGN_ALL -> KaEndpointUnificationSubstitutorPolicy.ASSIGN_ALL
}
