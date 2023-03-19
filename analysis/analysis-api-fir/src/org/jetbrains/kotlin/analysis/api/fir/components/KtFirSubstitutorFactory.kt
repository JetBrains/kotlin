/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KtSubstitutorBuilder
import org.jetbrains.kotlin.analysis.api.components.KtSubstitutorFactory
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.fir.types.KtFirMapBackedSubstitutor
import org.jetbrains.kotlin.analysis.api.fir.types.KtFirType
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap

internal class KtFirSubstitutorFactory(
    override val analysisSession: KtFirAnalysisSession
) : KtSubstitutorFactory(), KtFirAnalysisSessionComponent {

    override fun buildSubstitutor(builder: KtSubstitutorBuilder): KtSubstitutor {
        if (builder.mappings.isEmpty()) return KtSubstitutor.Empty(token)

        val firSubstitution = buildMap {
            builder.mappings.forEach { (ktTypeParameterSymbol, ktType) ->
                check(ktTypeParameterSymbol is KtFirTypeParameterSymbol)
                check(ktType is KtFirType)
                put(ktTypeParameterSymbol.firSymbol, ktType.coneType)
            }
        }

        val coneSubstitutor = ConeSubstitutorByMap(firSubstitution, analysisSession.useSiteSession)
        return KtFirMapBackedSubstitutor(coneSubstitutor, analysisSession.firSymbolBuilder)
    }
}