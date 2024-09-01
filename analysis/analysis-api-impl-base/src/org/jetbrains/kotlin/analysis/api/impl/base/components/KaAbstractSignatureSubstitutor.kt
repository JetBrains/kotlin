/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaSignatureSubstitutor
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError

@KaImplementationDetail
abstract class KaAbstractSignatureSubstitutor<T : KaSession> : KaSessionComponent<T>(), KaSignatureSubstitutor {
    override fun <S : KaFunctionSymbol> S.substitute(substitutor: KaSubstitutor): KaFunctionSignature<S> = withValidityAssertion {
        if (substitutor is KaSubstitutor.Empty) return asSignature()
        return asSignature().substitute(substitutor)
    }

    override fun <S : KaVariableSymbol> S.substitute(substitutor: KaSubstitutor): KaVariableSignature<S> = withValidityAssertion {
        if (substitutor is KaSubstitutor.Empty) return asSignature()
        return asSignature().substitute(substitutor)
    }

    override fun <S : KaCallableSymbol> S.substitute(substitutor: KaSubstitutor): KaCallableSignature<S> = withValidityAssertion {
        when (this) {
            is KaFunctionSymbol -> substitute(substitutor)
            is KaVariableSymbol -> substitute(substitutor)
            else -> unexpectedElementError("symbol", this)
        }
    }

    override fun <S : KaCallableSymbol> S.asSignature(): KaCallableSignature<S> = withValidityAssertion {
        return when (this) {
            is KaFunctionSymbol -> asSignature()
            is KaVariableSymbol -> asSignature()
            else -> unexpectedElementError("symbol", this)
        }
    }
}