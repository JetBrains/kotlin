/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.scopes

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.name.Name

/**
 * A [KaScope] provides access to the declarations contained in a specific [declaration][org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol],
 * file, or package, such as classes, type aliases, functions, properties, and constructors.
 *
 * To retrieve a scope for a particular declaration, use the various functions available in [KaScopeProvider][org.jetbrains.kotlin.analysis.api.components.KaScopeProvider].
 */
@OptIn(KaExperimentalApi::class)
public interface KaScope : KaScopeLike {
    /**
     * A sequence of all [KaDeclarationSymbol]s contained in the scope.
     */
    public val declarations: Sequence<KaDeclarationSymbol>
        get() = withValidityAssertion {
            sequence {
                yieldAll(callables)
                yieldAll(classifiers)
                yieldAll(constructors)
            }
        }

    /**
     * A sequence of [KaCallableSymbol]s contained in the scope.
     *
     * The implementation of this property needs to retrieve a set of all possible names before processing callables. The overload with
     * `Collection<Name>` should be used when the candidate name set is known.
     */
    public val callables: Sequence<KaCallableSymbol>
        get() = callables { true }

    /**
     * Returns a sequence of [KaCallableSymbol]s contained in the scope which match the [nameFilter].
     *
     * The implementation of this function needs to retrieve a set of all possible names before processing callables. The overload with
     * `Collection<Name>` should be used when the candidate name set is known.
     */
    public fun callables(nameFilter: (Name) -> Boolean): Sequence<KaCallableSymbol>

    /**
     * Returns a sequence of [KaCallableSymbol]s contained in the scope which match the given [names].
     *
     * The implementation of this function is optimized compared to using a name filter and should be used when the candidate name set is
     * known.
     */
    public fun callables(names: Collection<Name>): Sequence<KaCallableSymbol>

    /**
     * Returns a sequence of [KaCallableSymbol]s contained in the scope which match the given [names].
     *
     * The implementation of this function is optimized compared to using a name filter and should be used when the candidate name set is
     * known.
     */
    public fun callables(vararg names: Name): Sequence<KaCallableSymbol> =
        callables(names.toList())

    /**
     * A sequence of [KaClassifierSymbol]s contained in the scope.
     *
     * The result includes:
     *
     * - Nested classes
     * - Inner classes
     * - Nested type aliases for a class scope
     * - Top-level classes and top-level type aliases for a file scope
     *
     * The implementation of this property needs to retrieve a set of all possible names before processing classifiers. The overload with
     * `Collection<Name>` should be used when the candidate name set is known.
     */
    public val classifiers: Sequence<KaClassifierSymbol>
        get() = classifiers { true }

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
    public fun classifiers(nameFilter: (Name) -> Boolean): Sequence<KaClassifierSymbol>

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
    public fun classifiers(names: Collection<Name>): Sequence<KaClassifierSymbol>

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
    public fun classifiers(vararg names: Name): Sequence<KaClassifierSymbol> =
        classifiers(names.toList())

    /**
     * A sequence of [KaConstructorSymbol] contained in the scope.
     */
    public val constructors: Sequence<KaConstructorSymbol>

    /**
     * Returns a sequence of [KaPackageSymbol]s matching [nameFilter] which are a direct subpackage of the scope's package.
     */
    @KaExperimentalApi
    public fun getPackageSymbols(nameFilter: (Name) -> Boolean = { true }): Sequence<KaPackageSymbol>
}
