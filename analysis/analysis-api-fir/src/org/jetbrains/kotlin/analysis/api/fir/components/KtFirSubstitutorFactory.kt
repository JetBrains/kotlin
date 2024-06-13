/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaSubstitutorBuilder
import org.jetbrains.kotlin.analysis.api.components.KaSubstitutorFactory
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirGenericSubstitutor
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirMapBackedSubstitutor
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap

internal class KaFirSubstitutorFactory(
    override val analysisSession: KaFirSession
) : KaSubstitutorFactory(), KaFirSessionComponent {

    override fun buildSubstitutor(builder: KaSubstitutorBuilder): KaSubstitutor {
        if (builder.mappings.isEmpty()) return KaSubstitutor.Empty(token)

        val firSubstitution = buildMap {
            builder.mappings.forEach { (ktTypeParameterSymbol, ktType) ->
                check(ktTypeParameterSymbol is KaFirTypeParameterSymbol)
                check(ktType is KaFirType)
                put(ktTypeParameterSymbol.firSymbol, ktType.coneType)
            }
        }

        return when (val coneSubstitutor = ConeSubstitutorByMap.create(firSubstitution, analysisSession.useSiteSession)) {
            is ConeSubstitutorByMap -> KaFirMapBackedSubstitutor(coneSubstitutor, analysisSession.firSymbolBuilder)
            else -> KaFirGenericSubstitutor(coneSubstitutor, analysisSession.firSymbolBuilder)
        }
    }
}
