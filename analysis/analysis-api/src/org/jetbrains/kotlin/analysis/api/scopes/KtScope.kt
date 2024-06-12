/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.scopes

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.name.Name


public interface KaScope : KaScopeLike {
    /**
     * Return a sequence of all [KaDeclarationSymbol] which current scope contain
     */
    public val declarations: Sequence<KaDeclarationSymbol>
        get() = withValidityAssertion {
            sequence {
                yieldAll(callables())
                yieldAll(classifiers())
                yieldAll(constructors)
            }
        }

    @Deprecated("Use 'declarations' instead.", replaceWith = ReplaceWith("declarations"))
    public fun getAllSymbols(): Sequence<KaDeclarationSymbol> = declarations

    /**
     * Return a sequence of [KaCallableSymbol] which current scope contain if declaration name matches [nameFilter].
     *
     * This function needs to retrieve a set of all possible names before processing the scope.
     * The overload with `names: Collection<Name>` should be used when the candidate name set is known.
     */
    public fun callables(nameFilter: KaScopeNameFilter = { true }): Sequence<KaCallableSymbol>

    @Deprecated("Use 'callables' instead.", replaceWith = ReplaceWith("callables(nameFilter)"))
    public fun getCallableSymbols(nameFilter: KaScopeNameFilter = { true }): Sequence<KaCallableSymbol> = callables(nameFilter)

    /**
     * Return a sequence of [KaCallableSymbol] which current scope contain, if declaration name present in [names]
     *
     * This implementation is more optimal than the one with `nameFilter` and  should be used when the candidate name set is known.
     */
    public fun callables(names: Collection<Name>): Sequence<KaCallableSymbol>

    @Deprecated("Use 'callables' instead.", replaceWith = ReplaceWith("callables(names)"))
    public fun getCallableSymbols(names: Collection<Name>): Sequence<KaCallableSymbol> = callables(names)

    /**
     * Return a sequence of [KaCallableSymbol] which current scope contain, if declaration name present in [names]
     *
     * @see getCallableSymbols
     */
    public fun callables(vararg names: Name): Sequence<KaCallableSymbol> =
        callables(names.toList())

    @Deprecated("Use 'callables' instead.", replaceWith = ReplaceWith("callables(names)"))
    public fun getCallableSymbols(vararg names: Name): Sequence<KaCallableSymbol> = callables(names.toList())

    /**
     * Return a sequence of [KaClassifierSymbol] which current scope contain if classifier name matches [nameFilter]. The sequence includes:
     * nested classes, inner classes, nested type aliases for the class scope, and top-level classes and top-level type aliases for file scope.
     *
     * This function needs to retrieve a set of all possible names before processing the scope.
     * The overload with `names: Collection<Name>` should be used when the candidate name set is known.
     */
    public fun classifiers(nameFilter: KaScopeNameFilter = { true }): Sequence<KaClassifierSymbol>

    @Deprecated("Use 'classifiers' instead.", replaceWith = ReplaceWith("classifiers(nameFilter)"))
    public fun getClassifierSymbols(nameFilter: KaScopeNameFilter = { true }): Sequence<KaClassifierSymbol> = classifiers(nameFilter)

    /**
     * Return a sequence of [KaClassifierSymbol] which current scope contains, if classifier name present in [names].
     *
     * The sequence includes: nested classes, inner classes, nested type aliases for the class scope,
     * and top-level classes and top-level type aliases for file scope.
     *
     * This implementation is more optimal than the one with `nameFilter` and  should be used when the candidate name set is known.
     */
    public fun classifiers(names: Collection<Name>): Sequence<KaClassifierSymbol>

    @Deprecated("Use 'classifiers' instead.", replaceWith = ReplaceWith("classifiers(names)"))
    public fun getClassifierSymbols(names: Collection<Name>): Sequence<KaClassifierSymbol> = classifiers(names)

    /**
     * Return a sequence of [KaClassifierSymbol] which current scope contains, if classifier name present in [names].
     *
     * @see getClassifierSymbols
     */
    public fun classifiers(vararg names: Name): Sequence<KaClassifierSymbol> =
        classifiers(names.toList())

    @Deprecated("Use 'classifiers' instead.", replaceWith = ReplaceWith("classifiers(names)"))
    public fun getClassifierSymbols(vararg names: Name): Sequence<KaClassifierSymbol> = classifiers(names.asList())

    /**
     * Return a sequence of [KaConstructorSymbol] which current scope contain
     */
    public val constructors: Sequence<KaConstructorSymbol>

    /**
     * Return a sequence of [KaPackageSymbol] nested in current scope contain if package name matches [nameFilter]
     */
    public fun getPackageSymbols(nameFilter: KaScopeNameFilter = { true }): Sequence<KaPackageSymbol>
}

public typealias KtScope = KaScope
