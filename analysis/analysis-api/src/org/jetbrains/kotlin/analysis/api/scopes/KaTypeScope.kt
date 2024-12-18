/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.scopes

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.name.Name

/**
 * A scope which contains members whose use-site type parameters have been substituted with the type arguments of a concrete
 * [KaType][org.jetbrains.kotlin.analysis.api.types.KaType]. Callable declarations with substituted type parameters are represented as
 * [KaCallableSignature].
 *
 * @see org.jetbrains.kotlin.analysis.api.components.KaScopeProvider.scope
 */
@KaExperimentalApi
public interface KaTypeScope : KaScopeLike {
    /**
     * Returns a sequence of [KaCallableSignature]s contained in the scope which match the [nameFilter].
     *
     * The implementation of this function needs to retrieve a set of all possible names before processing callables. The overload with
     * `Collection<Name>` should be used when the candidate name set is known.
     */
    public fun getCallableSignatures(nameFilter: (Name) -> Boolean = { true }): Sequence<KaCallableSignature<*>>

    /**
     * Returns a sequence of [KaCallableSignature]s contained in the scope which match the given [names].
     *
     * The implementation of this function is optimized compared to using a name filter and should be used when the candidate name set is
     * known.
     */
    public fun getCallableSignatures(names: Collection<Name>): Sequence<KaCallableSignature<*>>

    /**
     * Returns a sequence of [KaCallableSignature]s contained in the scope which match the given [names].
     *
     * The implementation of this function is optimized compared to using a name filter and should be used when the candidate name set is
     * known.
     */
    public fun getCallableSignatures(vararg names: Name): Sequence<KaCallableSignature<*>> = withValidityAssertion {
        getCallableSignatures(names.toList())
    }

    /**
     * Returns a sequence of [KaClassifierSymbol]s contained in the scope which match the [nameFilter].
     *
     * The result includes:
     *
     * - Nested classes
     * - Inner classes
     * - Nested type aliases for a class scope
     * - Top-level classes and top-level type aliases for a file scope
     *
     * The implementation of this function needs to retrieve a set of all possible names before processing classifiers. The overload with
     * `Collection<Name>` should be used when the candidate name set is known.
     */
    public fun getClassifierSymbols(nameFilter: (Name) -> Boolean = { true }): Sequence<KaClassifierSymbol>

    /**
     * Returns a sequence of [KaClassifierSymbol]s contained in the scope which match the given [names].
     *
     * The result includes:
     *
     * - Nested classes
     * - Inner classes
     * - Nested type aliases for a class scope
     * - Top-level classes and top-level type aliases for a file scope
     *
     * The implementation of this function is optimized compared to using a name filter and should be used when the candidate name set is
     * known.
     */
    public fun getClassifierSymbols(names: Collection<Name>): Sequence<KaClassifierSymbol>

    /**
     * Returns a sequence of [KaClassifierSymbol]s contained in the scope which match the given [names].
     *
     * The result includes:
     *
     * - Nested classes
     * - Inner classes
     * - Nested type aliases for a class scope
     * - Top-level classes and top-level type aliases for a file scope
     *
     * The implementation of this function is optimized compared to using a name filter and should be used when the candidate name set is
     * known.
     */
    public fun getClassifierSymbols(vararg names: Name): Sequence<KaClassifierSymbol> =
        getClassifierSymbols(names.toList())

    /**
     * Returns a sequence of [KaConstructorSymbol]s contained in the scope.
     */
    public fun getConstructors(): Sequence<KaConstructorSymbol>
}
