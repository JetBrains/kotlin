/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.weakRef
import org.jetbrains.kotlin.analysis.api.impl.base.KtMapBackedSubstitutor
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap

internal abstract class AbstractKtFirSubstitutor<T : ConeSubstitutor>(
    private val _substitutor: T,
    builder: KtSymbolByFirBuilder,
    override val token: ValidityToken,
) : KtSubstitutor {
    protected val builderRef by weakRef(builder)
    val substitutor: T get() = withValidityAssertion { _substitutor }

    override fun substituteOrNull(type: KtType): KtType? = withValidityAssertion {
        require(type is KtFirType)
        substitutor.substituteOrNull(type.coneType)?.type?.let { builderRef.typeBuilder.buildKtType(it) }
    }
}

internal class KtFirGenericSubstitutor(
    _substitutor: ConeSubstitutor,
    builder: KtSymbolByFirBuilder,
    token: ValidityToken
) : AbstractKtFirSubstitutor<ConeSubstitutor>(_substitutor, builder, token)

internal class KtFirMapBackedSubstitutor(
    _substitutor: ConeSubstitutorByMap,
    builder: KtSymbolByFirBuilder,
    token: ValidityToken
) : AbstractKtFirSubstitutor<ConeSubstitutorByMap>(_substitutor, builder, token), KtMapBackedSubstitutor {
    override fun getAsMap(): Map<KtTypeParameterSymbol, KtType> {
        val result = mutableMapOf<KtTypeParameterSymbol, KtType>()
        for ((typeParameter, type) in substitutor.substitution) {
            val typeParameterSymbol = builderRef.classifierBuilder.buildTypeParameterSymbolByLookupTag(typeParameter.toLookupTag())
            if (typeParameterSymbol != null) {
                result[typeParameterSymbol] = builderRef.typeBuilder.buildKtType(type)
            }
        }

        return result
    }
}