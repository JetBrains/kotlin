/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.signatures

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.CallableId

/**
 * A use-site signature for a [callable symbol][KaCallableSymbol]. Compared to the symbol, the signature carries additional use-site type
 * information.
 *
 * The equality of [KaCallableSignature] is derived from its content.
 *
 * #### Example
 *
 * ```kotlin
 * fun test(l: List<String>) {
 *   l.get(1)
 * }
 * ```
 *
 * The [callable symbol][KaCallableSymbol] for `get` has the type `(Int) -> T` where `T` is the type parameter declared in `List`. On the
 * other hand, a [KaCallableSignature] for `l.get` carries the instantiated type information `(Int) -> String`.
 */
public sealed interface KaCallableSignature<out S : KaCallableSymbol> : KaLifetimeOwner {
    /**
     * The underlying symbol which the signature carries use-site information about.
     */
    public val symbol: S

    /**
     * The use-site-substituted [return type][KaCallableSymbol.returnType].
     */
    public val returnType: KaType

    /**
     * The use-site-substituted [extension receiver type][KaCallableSymbol.receiverParameter].
     */
    public val receiverType: KaType?

    /**
     * The [CallableId] of the signature, corresponding to the symbol's callable ID.
     */
    public val callableId: CallableId? get() = withValidityAssertion { symbol.callableId }

    /**
     * The use-site-substituted [context parameters][org.jetbrains.kotlin.analysis.api.symbols.contextParameters].
     */
    @KaExperimentalApi
    public val contextParameters: List<KaVariableSignature<KaContextParameterSymbol>> get() = withValidityAssertion { emptyList() }

    /**
     * Applies the given [substitutor] to the signature, returning a new signature with substituted types.
     *
     * @see KaSubstitutor.substitute
     */
    @KaExperimentalApi
    public fun substitute(substitutor: KaSubstitutor): KaCallableSignature<S>

    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
}
