/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.fir.KaChainedSubstitutor
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSessionBoundOwner
import org.jetbrains.kotlin.analysis.api.fir.KaMapBackedSubstitutor
import org.jetbrains.kotlin.analysis.api.fir.components.firSymbolBuilder
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.fir.utils.toKaSubstitutor
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap

internal abstract class AbstractKaFirSubstitutor<T : ConeSubstitutor>(
    val substitutor: T,
    override val analysisSession: KaFirSession,
) : KaSubstitutor, KaFirSessionBoundOwner {

    override fun substituteOrNull(type: KaType): KaType? = entryPointImplementation {
        require(type is KaFirType)
        substitutor.substituteOrNull(type.coneType)?.let { firSymbolBuilder.typeBuilder.buildKtType(it) }
    }
}

internal class KaFirEmptySubstitutor(override val token: KaLifetimeToken) : KaSubstitutor.Empty {
    override fun substitute(type: KaType): KaType = withValidityAssertion { type }
    override fun substituteOrNull(type: KaType): KaType? = withValidityAssertion { null }
}

internal class KaFirGenericSubstitutor(
    substitutor: ConeSubstitutor,
    analysisSession: KaFirSession,
) : AbstractKaFirSubstitutor<ConeSubstitutor>(substitutor, analysisSession)

internal class KaFirMapBackedSubstitutor(
    substitutor: ConeSubstitutorByMap,
    analysisSession: KaFirSession,
) : AbstractKaFirSubstitutor<ConeSubstitutorByMap>(substitutor, analysisSession), KaMapBackedSubstitutor {
    override fun getAsMap(): Map<KaTypeParameterSymbol, KaType> = entryPointImplementation {
        val result = mutableMapOf<KaTypeParameterSymbol, KaType>()
        for ([typeParameter, type] in substitutor.substitution) {
            val typeParameterSymbol = firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(typeParameter)
            result[typeParameterSymbol] = firSymbolBuilder.typeBuilder.buildKtType(type)
        }

        return result
    }
}

internal class KaFirChainedSubstitutor(
    substitutor: ChainedSubstitutor,
    analysisSession: KaFirSession,
) : AbstractKaFirSubstitutor<ChainedSubstitutor>(substitutor, analysisSession), KaChainedSubstitutor {
    override val first: KaSubstitutor
        get() = entryPointImplementation { substitutor.first.toKaSubstitutor() }

    override val second: KaSubstitutor
        get() = entryPointImplementation { substitutor.second.toKaSubstitutor() }
}
