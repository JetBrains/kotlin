/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.scopes

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.name.Name

@OptIn(KaExperimentalApi::class)
public interface KaScope : KaScopeLike {
    /**
     * A sequence of all [KaDeclarationSymbol]s contained in this scope.
     */
    public val declarations: Sequence<KaDeclarationSymbol>
        get() = withValidityAssertion {
            sequence {
                yieldAll(callables)
                yieldAll(classifiers)
                yieldAll(constructors)
            }
        }

    @Deprecated("Use 'declarations' instead.", replaceWith = ReplaceWith("declarations"))
    public fun getAllSymbols(): Sequence<KaDeclarationSymbol> = declarations

    /**
     * A sequence of [KaCallableSymbol]s contained in this scope.
     *
     * This property needs to retrieve a set of all possible names before processing the scope.
     * The overload with `names: Collection<Name>` should be used when the candidate name set is known.
     */
    public val callables: Sequence<KaCallableSymbol>
        get() = callables { true }

    /**
     * Returns a sequence of [KaCallableSymbol]s contained in this scope with callable names matching [nameFilter].
     *
     * This function needs to retrieve a set of all possible names before processing the scope.
     * The overload with `names: Collection<Name>` should be used when the candidate name set is known.
     */
    public fun callables(nameFilter: (Name) -> Boolean): Sequence<KaCallableSymbol>

    @Deprecated("Use 'callables' instead.", replaceWith = ReplaceWith("callables(nameFilter)"))
    public fun getCallableSymbols(nameFilter: (Name) -> Boolean = { true }): Sequence<KaCallableSymbol> = callables(nameFilter)

    /**
     * Returns a sequence of [KaCallableSymbol]s contained in this scope, limited to callables whose name is contained in [names].
     *
     * This implementation is more optimal than the one with `nameFilter` and should be used when the candidate name set is known.
     */
    public fun callables(names: Collection<Name>): Sequence<KaCallableSymbol>

    @Deprecated("Use 'callables' instead.", replaceWith = ReplaceWith("callables(names)"))
    public fun getCallableSymbols(names: Collection<Name>): Sequence<KaCallableSymbol> = callables(names)

    /**
     * Returns a sequence of [KaCallableSymbol]s contained in this scope, limited to callables whose name is contained in [names].
     *
     * This implementation is more optimal than the one with `nameFilter` and should be used when the candidate name set is known.
     */
    public fun callables(vararg names: Name): Sequence<KaCallableSymbol> =
        callables(names.toList())

    @Deprecated("Use 'callables' instead.", replaceWith = ReplaceWith("callables(names)"))
    public fun getCallableSymbols(vararg names: Name): Sequence<KaCallableSymbol> = callables(names.toList())

    /**
     * A sequence of [KaClassifierSymbol]s contained in this scope.
     *
     * The sequence includes: nested classes, inner classes, nested type aliases for the class scope, as well as top-level classes and
     * top-level type aliases for file scopes.
     *
     * This property needs to retrieve a set of all possible names before processing the scope.
     * The overload with `names: Collection<Name>` should be used when the candidate name set is known.
     */
    public val classifiers: Sequence<KaClassifierSymbol>
        get() = classifiers { true }

    /**
     * Returns a sequence of [KaClassifierSymbol]s contained in this scope with classifier names matching [nameFilter].
     *
     * The sequence includes: nested classes, inner classes, nested type aliases for the class scope, as well as top-level classes and
     * top-level type aliases for file scopes.
     *
     * This function needs to retrieve a set of all possible names before processing the scope.
     * The overload with `names: Collection<Name>` should be used when the candidate name set is known.
     */
    public fun classifiers(nameFilter: (Name) -> Boolean): Sequence<KaClassifierSymbol>

    @Deprecated("Use 'classifiers' instead.", replaceWith = ReplaceWith("classifiers(nameFilter)"))
    public fun getClassifierSymbols(nameFilter: (Name) -> Boolean = { true }): Sequence<KaClassifierSymbol> = classifiers(nameFilter)

    /**
     * Returns a sequence of [KaClassifierSymbol]s contained in this scope, limited to classifiers whose name is contained in [names].
     *
     * The sequence includes: nested classes, inner classes, nested type aliases for the class scope, as well as top-level classes and
     * top-level type aliases for file scopes.
     *
     * This implementation is more optimal than the one with `nameFilter` and should be used when the candidate name set is known.
     */
    public fun classifiers(names: Collection<Name>): Sequence<KaClassifierSymbol>

    @Deprecated("Use 'classifiers' instead.", replaceWith = ReplaceWith("classifiers(names)"))
    public fun getClassifierSymbols(names: Collection<Name>): Sequence<KaClassifierSymbol> = classifiers(names)

    /**
     * Returns a sequence of [KaClassifierSymbol]s contained in this scope, limited to classifiers whose name is contained in [names].
     *
     * The sequence includes: nested classes, inner classes, nested type aliases for the class scope, as well as top-level classes and
     * top-level type aliases for file scopes.
     *
     * This implementation is more optimal than the one with `nameFilter` and should be used when the candidate name set is known.
     */
    public fun classifiers(vararg names: Name): Sequence<KaClassifierSymbol> =
        classifiers(names.toList())

    @Deprecated("Use 'classifiers' instead.", replaceWith = ReplaceWith("classifiers(names)"))
    public fun getClassifierSymbols(vararg names: Name): Sequence<KaClassifierSymbol> = classifiers(names.asList())

    /**
     * A sequence of [KaConstructorSymbol] contained in this scope.
     */
    public val constructors: Sequence<KaConstructorSymbol>

    /**
     * Return a sequence of [KaPackageSymbol] nested in current scope contain if package name matches [nameFilter]
     */
    @KaExperimentalApi
    public fun getPackageSymbols(nameFilter: (Name) -> Boolean = { true }): Sequence<KaPackageSymbol>
}

@Deprecated("Use 'KaScope' instead.", replaceWith = ReplaceWith("KaScope"))
public typealias KtScope = KaScope
