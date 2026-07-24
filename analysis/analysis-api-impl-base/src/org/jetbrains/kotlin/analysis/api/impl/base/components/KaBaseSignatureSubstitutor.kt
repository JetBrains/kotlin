/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsSignatureSubstitutor
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor

@KaImplementationDetail
abstract class KaBaseSignatureSubstitutor<T : KaSession> : KaBaseSessionComponent<T>(), KaInternalsSignatureSubstitutor {
    override fun <S : KaFunctionSymbol> substitute(symbol: S, substitutor: KaSubstitutor): KaFunctionSignature<S> =
        symbol.withValidityAssertion {
            if (substitutor is KaSubstitutor.Empty) return asSignature(symbol)
            return asSignature(symbol).substitute(substitutor)
        }

    override fun <S : KaVariableSymbol> substitute(symbol: S, substitutor: KaSubstitutor): KaVariableSignature<S> =
        symbol.withValidityAssertion {
            if (substitutor is KaSubstitutor.Empty) return asSignature(symbol)
            return asSignature(symbol).substitute(substitutor)
        }

    override fun <S : KaCallableSymbol> substitute(symbol: S, substitutor: KaSubstitutor): KaCallableSignature<S> =
        symbol.withValidityAssertion {
            when (symbol) {
                is KaFunctionSymbol -> substitute(symbol, substitutor)
                is KaVariableSymbol -> substitute(symbol, substitutor)
            }
        }

    override fun <S : KaCallableSymbol> asSignature(symbol: S): KaCallableSignature<S> = symbol.withValidityAssertion {
        return when (symbol) {
            is KaFunctionSymbol -> asSignature(symbol)
            is KaVariableSymbol -> asSignature(symbol)
        }
    }
}
