/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.impl.base.KaChainedSubstitutor
import org.jetbrains.kotlin.analysis.api.impl.base.KaMapBackedSubstitutor
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
    protected val builder: KaSymbolByFirBuilder,
) : KaSubstitutor {
    override val token: KaLifetimeToken get() = builder.token

    override fun substituteOrNull(type: KaType): KaType? = withValidityAssertion {
        require(type is KaFirType)
        substitutor.substituteOrNull(type.coneType)?.type?.let { builder.typeBuilder.buildKtType(it) }
    }
}

internal class KaFirGenericSubstitutor(
    substitutor: ConeSubstitutor,
    builder: KaSymbolByFirBuilder,
) : AbstractKaFirSubstitutor<ConeSubstitutor>(substitutor, builder)

internal class KaFirMapBackedSubstitutor(
    substitutor: ConeSubstitutorByMap,
    builder: KaSymbolByFirBuilder,
) : AbstractKaFirSubstitutor<ConeSubstitutorByMap>(substitutor, builder,), KaMapBackedSubstitutor {
    override fun getAsMap(): Map<KaTypeParameterSymbol, KaType> = withValidityAssertion {
        val result = mutableMapOf<KaTypeParameterSymbol, KaType>()
        for ((typeParameter, type) in substitutor.substitution) {
            val typeParameterSymbol = builder.classifierBuilder.buildTypeParameterSymbol(typeParameter)
            result[typeParameterSymbol] = builder.typeBuilder.buildKtType(type)
        }

        return result
    }
}

internal class KaFirChainedSubstitutor(
    substitutor: ChainedSubstitutor,
    builder: KaSymbolByFirBuilder,
) : AbstractKaFirSubstitutor<ChainedSubstitutor>(substitutor, builder), KaChainedSubstitutor {
    override val first
        get(): KaSubstitutor {
            return builder.typeBuilder.buildSubstitutor(substitutor.first)
        }

    override val second
        get(): KaSubstitutor {
            return builder.typeBuilder.buildSubstitutor(substitutor.second)
        }
}