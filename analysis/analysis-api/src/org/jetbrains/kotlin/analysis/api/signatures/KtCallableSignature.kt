/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.signatures

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.name.CallableId

/**
 * A signature for a callable symbol. Comparing to a `KtCallableSymbol`, a signature can carry use-site type information. For example
 * ```
 * fun test(l: List<String>) {
 *   l.get(1) // The symbol `get` has type `(Int) -> T` where is the type parameter declared in `List`. On the other hand, a KtSignature
 *            // carries instantiated type information `(Int) -> String`.
 * }
 * ```
 *
 * Equality of [KtCallableSignature] is derived from its content.
 */
public sealed class KtCallableSignature<out S : KtCallableSymbol> : KtLifetimeOwner {
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

    /**
     * A [CallableId] of a substituted symbol
     */
    public open val callableIdIfNonLocal: CallableId? get() = withValidityAssertion { symbol.callableIdIfNonLocal }

    /**
     * Applies a [substitutor] to the given signature and return a new signature with substituted types.
     *
     * @see KtSubstitutor.substitute
     */
    public abstract fun substitute(substitutor: KtSubstitutor): KtCallableSignature<S>

    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
}

