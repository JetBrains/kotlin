/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.impl.base.KtChainedSubstitutor
import org.jetbrains.kotlin.analysis.api.impl.base.KtMapBackedSubstitutor
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap

internal abstract class AbstractKtFirSubstitutor<T : ConeSubstitutor>(
    val substitutor: T,
    protected val builder: KtSymbolByFirBuilder,
) : KtSubstitutor {
    override val token: KtLifetimeToken get() = builder.token

    override fun substituteOrNull(type: KtType): KtType? = withValidityAssertion {
        require(type is KtFirType)
        substitutor.substituteOrNull(type.coneType)?.type?.let { builder.typeBuilder.buildKtType(it) }
    }
}

internal class KtFirGenericSubstitutor(
    substitutor: ConeSubstitutor,
    builder: KtSymbolByFirBuilder,
) : AbstractKtFirSubstitutor<ConeSubstitutor>(substitutor, builder)

@OptIn(KtAnalysisApiInternals::class)
internal class KtFirMapBackedSubstitutor(
    substitutor: ConeSubstitutorByMap,
    builder: KtSymbolByFirBuilder,
) : AbstractKtFirSubstitutor<ConeSubstitutorByMap>(substitutor, builder,), KtMapBackedSubstitutor {
    override fun getAsMap(): Map<KtTypeParameterSymbol, KtType> = withValidityAssertion {
        val result = mutableMapOf<KtTypeParameterSymbol, KtType>()
        for ((typeParameter, type) in substitutor.substitution) {
            val typeParameterSymbol = builder.classifierBuilder.buildTypeParameterSymbolByLookupTag(typeParameter.toLookupTag())
            if (typeParameterSymbol != null) {
                result[typeParameterSymbol] = builder.typeBuilder.buildKtType(type)
            }
        }

        return result
    }
}

@OptIn(KtAnalysisApiInternals::class)
internal class KtFirChainedSubstitutor(
    substitutor: ChainedSubstitutor,
    builder: KtSymbolByFirBuilder,
) : AbstractKtFirSubstitutor<ChainedSubstitutor>(substitutor, builder), KtChainedSubstitutor {
    override val first
        get(): KtSubstitutor {
            return builder.typeBuilder.buildSubstitutor(substitutor.first)
        }

    override val second
        get(): KtSubstitutor {
            return builder.typeBuilder.buildSubstitutor(substitutor.second)
        }
}