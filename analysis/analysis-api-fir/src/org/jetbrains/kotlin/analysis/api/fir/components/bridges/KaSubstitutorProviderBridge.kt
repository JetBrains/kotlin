/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.components.KaSubstitutorProvider
import org.jetbrains.kotlin.analysis.api.components.KaUnificationSubstitutorPolicy
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsSubstitutorProvider
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.createInheritanceTypeSubstitutor as createInheritanceTypeSubstitutorEndpoint
import org.jetbrains.kotlin.analysis.api.types.createSubstitutor as createSubstitutorEndpoint

@OptIn(KaExperimentalApi::class, KaIdeApi::class)
internal class KaSubstitutorProviderBridge(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaSubstitutorProvider {
    private val proxy: KaInternalsSubstitutorProvider
        get() = analysisSession.substitutorProvider

    override fun createSubstitutor(mappings: Map<KaTypeParameterSymbol, KaType>): KaSubstitutor =
        context(analysisSession) { createSubstitutorEndpoint(mappings) }

    override fun createInheritanceTypeSubstitutor(subClass: KaClassSymbol, superClass: KaClassSymbol): KaSubstitutor? =
        context(analysisSession) { createInheritanceTypeSubstitutorEndpoint(subClass, superClass) }

    override fun createUnificationSubstitutor(
        candidateType: KaType,
        targetType: KaType,
        constructionPolicy: KaUnificationSubstitutorPolicy,
    ): KaSubstitutor? = context(analysisSession) {
        proxy.createSubtypingUnificationSubstitutor(candidateType, targetType, constructionPolicy)
    }

    override fun createUnificationSubstitutor(
        candidateTypesToTargetTypes: List<Pair<KaType, KaType>>,
        constructionPolicy: KaUnificationSubstitutorPolicy,
    ): KaSubstitutor? = context(analysisSession) {
        proxy.createSubtypingUnificationSubstitutor(candidateTypesToTargetTypes, constructionPolicy)
    }
}
