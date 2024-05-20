/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.signatures

import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.buildSymbol
import org.jetbrains.kotlin.analysis.api.fir.types.AbstractKaFirSubstitutor
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.receiverType
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

internal sealed class KaFirFunctionLikeSignature<out S : KaFunctionLikeSymbol> : KaFunctionLikeSignature<S>(), FirSymbolBasedSignature {
    abstract override fun substitute(substitutor: KaSubstitutor): KaFirFunctionLikeSignature<S>

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KaFirFunctionLikeSignature<*>
        return firSymbol == other.firSymbol
    }

    override fun hashCode(): Int = firSymbol.hashCode()
}

internal class KaFirFunctionLikeDummySignature<out S : KaFunctionLikeSymbol>(
    override val token: KaLifetimeToken,
    override val firSymbol: FirFunctionSymbol<*>,
    override val firSymbolBuilder: KaSymbolByFirBuilder,
) : KaFirFunctionLikeSignature<S>() {
    @Suppress("UNCHECKED_CAST")
    override val symbol: S
        get() = withValidityAssertion { firSymbol.buildSymbol(firSymbolBuilder) as S }
    override val returnType: KaType
        get() = withValidityAssertion { symbol.returnType }
    override val receiverType: KaType?
        get() = withValidityAssertion { symbol.receiverType }
    override val valueParameters: List<KaVariableLikeSignature<KaValueParameterSymbol>> by cached {
        firSymbol.valueParameterSymbols.map { KaFirVariableLikeDummySignature(token, it, firSymbolBuilder) }
    }

    override fun substitute(substitutor: KaSubstitutor): KaFirFunctionLikeSignature<S> = withValidityAssertion {
        if (substitutor is KaSubstitutor.Empty) return@withValidityAssertion this
        require(substitutor is AbstractKaFirSubstitutor<*>)

        KaFirFunctionLikeSubstitutorBasedSignature(token, firSymbol, firSymbolBuilder, substitutor.substitutor)
    }
}

internal class KaFirFunctionLikeSubstitutorBasedSignature<out S : KaFunctionLikeSymbol>(
    override val token: KaLifetimeToken,
    override val firSymbol: FirFunctionSymbol<*>,
    override val firSymbolBuilder: KaSymbolByFirBuilder,
    override val coneSubstitutor: ConeSubstitutor = ConeSubstitutor.Empty,
) : KaFirFunctionLikeSignature<S>(), SubstitutorBasedSignature {
    @Suppress("UNCHECKED_CAST")
    override val symbol: S
        get() = withValidityAssertion { firSymbol.buildSymbol(firSymbolBuilder) as S }
    override val returnType: KaType by cached {
        firSymbolBuilder.typeBuilder.buildKtType(coneSubstitutor.substituteOrSelf(firSymbol.resolvedReturnType))
    }
    override val receiverType: KaType? by cached {
        val receiverTypeRef = when (val fir = firSymbol.fir) {
            is FirPropertyAccessor -> fir.propertySymbol.resolvedReceiverTypeRef
            else -> firSymbol.resolvedReceiverTypeRef
        }
        receiverTypeRef?.let { firSymbolBuilder.typeBuilder.buildKtType(coneSubstitutor.substituteOrSelf(it.type)) }
    }
    override val valueParameters: List<KaVariableLikeSignature<KaValueParameterSymbol>> by cached {
        firSymbol.fir.valueParameters.map { firValueParameter ->
            KaFirVariableLikeSubstitutorBasedSignature(token, firValueParameter.symbol, firSymbolBuilder, coneSubstitutor)
        }
    }

    override fun substitute(substitutor: KaSubstitutor): KaFirFunctionLikeSignature<S> = withValidityAssertion {
        if (substitutor is KaSubstitutor.Empty) return@withValidityAssertion this
        require(substitutor is AbstractKaFirSubstitutor<*>)
        val chainedSubstitutor = ChainedSubstitutor(coneSubstitutor, substitutor.substitutor)

        KaFirFunctionLikeSubstitutorBasedSignature(token, firSymbol, firSymbolBuilder, chainedSubstitutor)
    }

    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) return false

        other as KaFirFunctionLikeSubstitutorBasedSignature<*>
        return coneSubstitutor == other.coneSubstitutor
    }

    override fun hashCode(): Int = 31 * super.hashCode() + coneSubstitutor.hashCode()
}