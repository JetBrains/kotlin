/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaSignatureSubstitutor
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError

@KaImplementationDetail
abstract class KaAbstractSignatureSubstitutor<T : KaSession> : KaSessionComponent<T>(), KaSignatureSubstitutor {
    override fun <S : KaFunctionLikeSymbol> S.substitute(substitutor: KaSubstitutor): KaFunctionLikeSignature<S> = withValidityAssertion {
        if (substitutor is KaSubstitutor.Empty) return asSignature()
        return asSignature().substitute(substitutor)
    }

    override fun <S : KaVariableLikeSymbol> S.substitute(substitutor: KaSubstitutor): KaVariableLikeSignature<S> = withValidityAssertion {
        if (substitutor is KaSubstitutor.Empty) return asSignature()
        return asSignature().substitute(substitutor)
    }

    override fun <S : KaCallableSymbol> S.substitute(substitutor: KaSubstitutor): KaCallableSignature<S> = withValidityAssertion {
        when (this) {
            is KaFunctionLikeSymbol -> substitute(substitutor)
            is KaVariableLikeSymbol -> substitute(substitutor)
            else -> unexpectedElementError("symbol", this)
        }
    }

    override fun <S : KaCallableSymbol> S.asSignature(): KaCallableSignature<S> = withValidityAssertion {
        return when (this) {
            is KaFunctionLikeSymbol -> asSignature()
            is KaVariableLikeSymbol -> asSignature()
            else -> unexpectedElementError("symbol", this)
        }
    }
}