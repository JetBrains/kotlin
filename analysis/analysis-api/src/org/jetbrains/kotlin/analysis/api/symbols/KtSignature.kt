/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.ValidityTokenOwner
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion

/**
 * A signature for a callable symbol. Comparing to a `KtCallableSymbol`, a signature can carry use-site type information. For example
 * ```
 * fun test(l: List<String>) {
 *   l.get(1) // The symbol `get` has type `(Int) -> T` where is the type parameter declared in `List`. On the other hand, a KtSignature
 *            // carries instantiated type information `(Int) -> String`.
 * }
 * ```
 *
 * Equality of [KtSignature] is derived from its content.
 */
public sealed class KtSignature<out S : KtCallableSymbol> : ValidityTokenOwner {
    /**
     * The original symbol for this signature.
     */
    public abstract val symbol: S

    /**
     * The use-site-substituted return type.
     */
    public abstract val returnType: KtType

    /**
     * The use-site-substituted extension receiver type.
     */
    public abstract val receiverType: KtType?

    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
}

/**
 * A signature of a function-like symbol.
 */
public data class KtFunctionLikeSignature<out S : KtFunctionLikeSymbol>(
    private val _symbol: S,
    private val _returnType: KtType,
    private val _receiverType: KtType?,
    private val _valueParameters: List<KtVariableLikeSignature<KtValueParameterSymbol>>,
) : KtSignature<S>() {
    override val token: ValidityToken
        get() = _symbol.token
    override val symbol: S
        get() = withValidityAssertion { _symbol }
    override val returnType: KtType
        get() = withValidityAssertion { _returnType }
    override val receiverType: KtType?
        get() = withValidityAssertion { _receiverType }

    /**
     * The use-site-substituted value parameters.
     */
    public val valueParameters: List<KtVariableLikeSignature<KtValueParameterSymbol>>
        get() = withValidityAssertion { _valueParameters }
}

/**
 * A signature of a variable-like symbol.
 */
public data class KtVariableLikeSignature<out S : KtVariableLikeSymbol>(
    private val _symbol: S,
    private val _returnType: KtType,
    private val _receiverType: KtType?,
) : KtSignature<S>() {
    override val token: ValidityToken
        get() = _symbol.token
    override val symbol: S
        get() = withValidityAssertion { _symbol }
    override val returnType: KtType
        get() = withValidityAssertion { _returnType }
    override val receiverType: KtType?
        get() = withValidityAssertion { _receiverType }
}

public fun <S : KtCallableSymbol> S.toSignature(substitutor: KtSubstitutor = KtSubstitutor.Empty(token)): KtSignature<S> {
    return when (this) {
        is KtVariableLikeSymbol -> toSignature(substitutor)
        is KtFunctionLikeSymbol -> toSignature(substitutor)
        else -> error("unexpected callable symbol $this")
    }
}

/**
 * Creates a signature with the same type as the given symbol.
 */
public fun <S : KtVariableLikeSymbol> S.toSignature(substitutor: KtSubstitutor = KtSubstitutor.Empty(token)): KtVariableLikeSignature<S> {
    return KtVariableLikeSignature(this, returnType, receiverType).substitute(substitutor)
}

/**
 * Creates a signature with the same type as the given symbol.
 */
public fun <S : KtFunctionLikeSymbol> S.toSignature(substitutor: KtSubstitutor = KtSubstitutor.Empty(token)): KtFunctionLikeSignature<S> {
    return KtFunctionLikeSignature(this, returnType, receiverType, valueParameters.map { it.toSignature() }).substitute(substitutor)
}

/**
 * Applies a substitutor to the given signature and return a new signature with substituted types.
 */
@Suppress("UNCHECKED_CAST")
public fun <C : KtSignature<S>, S : KtCallableSymbol> C.substitute(substitutor: KtSubstitutor): C {
    if (substitutor is KtSubstitutor.Empty) return this
    return when (this) {
        is KtFunctionLikeSignature<*> -> KtFunctionLikeSignature(
            symbol,
            substitutor.substituteOrSelf(returnType),
            receiverType?.let { substitutor.substituteOrSelf(it) },
            valueParameters.map { it.substitute(substitutor) }
        ) as C
        is KtVariableLikeSignature<*> -> KtVariableLikeSignature(
            symbol,
            substitutor.substituteOrSelf(returnType),
            receiverType?.let { substitutor.substituteOrSelf(it) },
        ) as C
        else -> error("impossible since KtSignature is sealed")
    }
}