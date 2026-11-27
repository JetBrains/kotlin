/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.signatures

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.buildSymbol
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.fir.types.AbstractKaFirSubstitutor
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.receiverType
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

internal sealed class KaFirFunctionSignature<out S : KaFunctionSymbol> : KaFunctionSignature<S>, FirSymbolBasedSignature {
    abstract override fun substitute(substitutor: KaSubstitutor): KaFirFunctionSignature<S>

    override fun equals(other: Any?): Boolean = this === other ||
            other?.javaClass == javaClass &&
            (other as KaFirFunctionSignature<*>).firSymbol == firSymbol

    override fun hashCode(): Int = firSymbol.hashCode()
}

internal class KaFirFunctionDummySignature<out S : KaFunctionSymbol>(
    override val firSymbol: FirFunctionSymbol<*>,
    override val analysisSession: KaFirSession,
) : KaFirFunctionSignature<S>() {
    @Suppress("UNCHECKED_CAST")
    override val symbol: S get() = entryPointImplementation { firSymbol.buildSymbol() as S }
    override val returnType: KaType get() = entryPointImplementation { symbol.returnType }
    override val receiverType: KaType? get() = entryPointImplementation { symbol.receiverType }
    override val valueParameters: List<KaVariableSignature<KaValueParameterSymbol>> by cached {
        firSymbol.fir.valueParameters.map { KaFirVariableDummySignature(it.symbol, analysisSession) }
    }

    override val contextParameters: List<KaVariableSignature<KaContextParameterSymbol>> by cached {
        firSymbol.fir.contextParameters.map { KaFirVariableDummySignature(it.symbol, analysisSession) }
    }

    override fun substitute(substitutor: KaSubstitutor): KaFirFunctionSignature<S> = entryPointImplementation {
        if (substitutor is KaSubstitutor.Empty) return@entryPointImplementation this
        require(substitutor is AbstractKaFirSubstitutor<*>)

        KaFirFunctionSubstitutorBasedSignature(firSymbol, analysisSession, substitutor.substitutor)
    }
}

internal class KaFirFunctionSubstitutorBasedSignature<out S : KaFunctionSymbol>(
    override val firSymbol: FirFunctionSymbol<*>,
    override val analysisSession: KaFirSession,
    override val coneSubstitutor: ConeSubstitutor = ConeSubstitutor.Empty,
) : KaFirFunctionSignature<S>(), SubstitutorBasedSignature {
    @Suppress("UNCHECKED_CAST")
    override val symbol: S get() = entryPointImplementation { firSymbol.buildSymbol() as S }

    override val returnType: KaType by cached {
        analysisSession.firSymbolBuilder.typeBuilder.buildKtType(coneSubstitutor.substituteOrSelf(firSymbol.resolvedReturnType))
    }

    override val receiverType: KaType? by cached {
        val receiverTypeRef = when (val fir = firSymbol.fir) {
            is FirPropertyAccessor -> fir.propertySymbol.resolvedReceiverTypeRef
            else -> firSymbol.resolvedReceiverTypeRef
        }

        receiverTypeRef?.let { analysisSession.firSymbolBuilder.typeBuilder.buildKtType(coneSubstitutor.substituteOrSelf(it.coneType)) }
    }

    override val valueParameters: List<KaVariableSignature<KaValueParameterSymbol>> by cached {
        firSymbol.fir.valueParameters.map {
            KaFirVariableSubstitutorBasedSignature(it.symbol, analysisSession, coneSubstitutor)
        }
    }

    override val contextParameters: List<KaVariableSignature<KaContextParameterSymbol>> by cached {
        firSymbol.fir.contextParameters.map {
            KaFirVariableSubstitutorBasedSignature(it.symbol, analysisSession, coneSubstitutor)
        }
    }

    override fun substitute(substitutor: KaSubstitutor): KaFirFunctionSignature<S> = entryPointImplementation {
        if (substitutor is KaSubstitutor.Empty) return@entryPointImplementation this
        require(substitutor is AbstractKaFirSubstitutor<*>)
        val chainedSubstitutor = ChainedSubstitutor(coneSubstitutor, substitutor.substitutor)

        KaFirFunctionSubstitutorBasedSignature(firSymbol, analysisSession, chainedSubstitutor)
    }

    override fun equals(other: Any?): Boolean =
        super.equals(other) && (other as KaFirFunctionSubstitutorBasedSignature<*>).coneSubstitutor == coneSubstitutor

    override fun hashCode(): Int = 31 * super.hashCode() + coneSubstitutor.hashCode()
}
