/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.signatures

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.CallableId

/**
 * A signature for a callable symbol. Comparing to a [KaCallableSymbol], a signature can carry use-site type information. For example
 * ```
 * fun test(l: List<String>) {
 *   l.get(1) // The symbol `get` has type `(Int) -> T` where is the type parameter declared in `List`.
 *            // On the other hand, a `KaCallableSignature` carries instantiated type information `(Int) -> String`.
 * }
 * ```
 *
 * Equality of [KaCallableSignature] is derived from its content.
 */
public sealed class KaCallableSignature<out S : KaCallableSymbol> : KaLifetimeOwner {
    /**
     * The original symbol for this signature.
     */
    public abstract val symbol: S

    /**
     * The use-site-substituted return type.
     */
    public abstract val returnType: KaType

    /**
     * The use-site-substituted extension receiver type.
     */
    public abstract val receiverType: KaType?

    /**
     * A [CallableId] of a substituted symbol
     */
    public open val callableId: CallableId? get() = withValidityAssertion { symbol.callableId }

    @Deprecated("Use `callableId` instead.", ReplaceWith("callableId"))
    public val callableIdIfNonLocal: CallableId? get() = callableId

    /**
     * Applies a [substitutor] to the given signature and return a new signature with substituted types.
     *
     * @see KaSubstitutor.substitute
     */
    public abstract fun substitute(substitutor: KaSubstitutor): KaCallableSignature<S>

    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
}

public typealias KtCallableSignature<S> = KaCallableSignature<S>