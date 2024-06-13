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
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.receiverType
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.arrayElementType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

internal sealed class KaFirVariableLikeSignature<out S : KaVariableLikeSymbol> : KaVariableLikeSignature<S>(), FirSymbolBasedSignature {
    abstract override fun substitute(substitutor: KaSubstitutor): KaFirVariableLikeSignature<S>

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KaFirVariableLikeSignature<*>
        return firSymbol == other.firSymbol
    }

    override fun hashCode(): Int = firSymbol.hashCode()
}

internal class KaFirVariableLikeDummySignature<out S : KaVariableLikeSymbol>(
    override val token: KaLifetimeToken,
    override val firSymbol: FirVariableSymbol<*>,
    override val firSymbolBuilder: KaSymbolByFirBuilder,
) : KaFirVariableLikeSignature<S>() {
    @Suppress("UNCHECKED_CAST")
    override val symbol: S
        get() = withValidityAssertion { firSymbol.buildSymbol(firSymbolBuilder) as S }
    override val returnType: KaType
        get() = withValidityAssertion { symbol.returnType }
    override val receiverType: KaType?
        get() = withValidityAssertion { symbol.receiverType }

    override fun substitute(substitutor: KaSubstitutor): KaFirVariableLikeSignature<S> = withValidityAssertion {
        if (substitutor is KaSubstitutor.Empty) return@withValidityAssertion this
        require(substitutor is AbstractKaFirSubstitutor<*>)

        KaFirVariableLikeSubstitutorBasedSignature(token, firSymbol, firSymbolBuilder, substitutor.substitutor)
    }
}

internal class KaFirVariableLikeSubstitutorBasedSignature<out S : KaVariableLikeSymbol>(
    override val token: KaLifetimeToken,
    override val firSymbol: FirVariableSymbol<*>,
    override val firSymbolBuilder: KaSymbolByFirBuilder,
    override val coneSubstitutor: ConeSubstitutor = ConeSubstitutor.Empty,
) : KaFirVariableLikeSignature<S>(), SubstitutorBasedSignature {
    @Suppress("UNCHECKED_CAST")
    override val symbol: S
        get() = withValidityAssertion { firSymbol.buildSymbol(firSymbolBuilder) as S }
    override val returnType: KaType by cached {
        val isVarargValueParameter = (firSymbol as? FirValueParameterSymbol)?.isVararg == true
        val coneType = firSymbol.resolvedReturnType.applyIf(isVarargValueParameter) { arrayElementType() ?: this }

        firSymbolBuilder.typeBuilder.buildKtType(coneSubstitutor.substituteOrSelf(coneType))
    }
    override val receiverType: KaType? by cached {
        firSymbol.resolvedReceiverTypeRef?.let { typeRef ->
            firSymbolBuilder.typeBuilder.buildKtType(coneSubstitutor.substituteOrSelf(typeRef.coneType))
        }
    }

    override fun substitute(substitutor: KaSubstitutor): KaFirVariableLikeSignature<S> = withValidityAssertion {
        if (substitutor is KaSubstitutor.Empty) return@withValidityAssertion this
        require(substitutor is AbstractKaFirSubstitutor<*>)
        val chainedSubstitutor = ChainedSubstitutor(coneSubstitutor, substitutor.substitutor)

        KaFirVariableLikeSubstitutorBasedSignature(token, firSymbol, firSymbolBuilder, chainedSubstitutor)
    }

    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) return false

        other as KaFirVariableLikeSubstitutorBasedSignature<*>
        return coneSubstitutor == other.coneSubstitutor
    }

    override fun hashCode(): Int = 31 * super.hashCode() + coneSubstitutor.hashCode()
}