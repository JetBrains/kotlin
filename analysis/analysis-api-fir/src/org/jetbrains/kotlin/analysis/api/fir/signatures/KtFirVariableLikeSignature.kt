/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.signatures

import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.buildSymbol
import org.jetbrains.kotlin.analysis.api.fir.types.AbstractKtFirSubstitutor
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.signatures.KtVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.receiverType
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.arrayElementType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

internal sealed class KtFirVariableLikeSignature<out S : KtVariableLikeSymbol> : KtVariableLikeSignature<S>(), FirSymbolBasedSignature {
    abstract override fun substitute(substitutor: KtSubstitutor): KtFirVariableLikeSignature<S>

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KtFirVariableLikeSignature<*>
        return firSymbol == other.firSymbol
    }

    override fun hashCode(): Int = firSymbol.hashCode()
}

internal class KtFirVariableLikeDummySignature<out S : KtVariableLikeSymbol>(
    override val token: KtLifetimeToken,
    override val firSymbol: FirVariableSymbol<*>,
    override val firSymbolBuilder: KtSymbolByFirBuilder,
) : KtFirVariableLikeSignature<S>() {
    @Suppress("UNCHECKED_CAST")
    override val symbol: S
        get() = withValidityAssertion { firSymbol.buildSymbol(firSymbolBuilder) as S }
    override val returnType: KtType
        get() = withValidityAssertion { symbol.returnType }
    override val receiverType: KtType?
        get() = withValidityAssertion { symbol.receiverType }

    override fun substitute(substitutor: KtSubstitutor): KtFirVariableLikeSignature<S> = withValidityAssertion {
        if (substitutor is KtSubstitutor.Empty) return@withValidityAssertion this
        require(substitutor is AbstractKtFirSubstitutor<*>)

        KtFirVariableLikeSubstitutorBasedSignature(token, firSymbol, firSymbolBuilder, substitutor.substitutor)
    }
}

internal class KtFirVariableLikeSubstitutorBasedSignature<out S : KtVariableLikeSymbol>(
    override val token: KtLifetimeToken,
    override val firSymbol: FirVariableSymbol<*>,
    override val firSymbolBuilder: KtSymbolByFirBuilder,
    override val coneSubstitutor: ConeSubstitutor = ConeSubstitutor.Empty,
) : KtFirVariableLikeSignature<S>(), SubstitutorBasedSignature {
    @Suppress("UNCHECKED_CAST")
    override val symbol: S
        get() = withValidityAssertion { firSymbol.buildSymbol(firSymbolBuilder) as S }
    override val returnType: KtType by cached {
        val isVarargValueParameter = (firSymbol as? FirValueParameterSymbol)?.isVararg == true
        val coneType = firSymbol.resolvedReturnType.applyIf(isVarargValueParameter) { arrayElementType() ?: this }

        firSymbolBuilder.typeBuilder.buildKtType(coneSubstitutor.substituteOrSelf(coneType))
    }
    override val receiverType: KtType? by cached {
        firSymbol.resolvedReceiverTypeRef?.let { typeRef ->
            firSymbolBuilder.typeBuilder.buildKtType(coneSubstitutor.substituteOrSelf(typeRef.coneType))
        }
    }

    override fun substitute(substitutor: KtSubstitutor): KtFirVariableLikeSignature<S> = withValidityAssertion {
        if (substitutor is KtSubstitutor.Empty) return@withValidityAssertion this
        require(substitutor is AbstractKtFirSubstitutor<*>)
        val chainedSubstitutor = ChainedSubstitutor(coneSubstitutor, substitutor.substitutor)

        KtFirVariableLikeSubstitutorBasedSignature(token, firSymbol, firSymbolBuilder, chainedSubstitutor)
    }

    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) return false

        other as KtFirVariableLikeSubstitutorBasedSignature<*>
        return coneSubstitutor == other.coneSubstitutor
    }

    override fun hashCode(): Int = 31 * super.hashCode() + coneSubstitutor.hashCode()
}