/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.scopes

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.signatures.KtCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.name.Name

/**
 * A scope inside which use-site type-parameters of callable declarations may be substituted. Such declarations are represented as [KtCallableSignature].
 *
 * @see org.jetbrains.kotlin.analysis.api.components.KtScopeProviderMixIn.getTypeScope
 * @see KtCallableSignature
 */
public interface KtTypeScope : KtScopeLike {

    /**
     * Return a sequence of [KtCallableSignature] which current scope contain if declaration name matches [nameFilter].
     */
    public fun getCallableSignatures(nameFilter: KtScopeNameFilter = { true }): Sequence<KtCallableSignature<*>>

    /**
     * Return a sequence of [KtCallableSignature] which current scope contain if declaration, if declaration name present in [names]
     *
     * This implementation is more optimal than the one with `nameFilter` and should be used when the candidate name set is known.
     */
    public fun getCallableSignatures(names: Collection<Name>): Sequence<KtCallableSignature<*>>

    /**
     * Return a sequence of [KtCallableSignature] which current scope contain if declaration, if declaration name present in [names]
     *
     * @see getCallableSignatures
     */
    public fun getCallableSignatures(vararg names: Name): Sequence<KtCallableSignature<*>> = withValidityAssertion {
        getCallableSignatures(names.toList())
    }

    /**
     * Return a sequence of [KtClassifierSymbol] which current scope contain if classifier name matches [nameFilter]. The sequence includes:
     * nested classes, inner classes, nested type aliases for the class scope, and top-level classes and top-level type aliases for file scope.
     *
     * This function needs to retrieve a set of all possible names before processing the scope.
     * The overload with `names: Collection<Name>` should be used when the candidate name set is known.
     */
    public fun getClassifierSymbols(nameFilter: KtScopeNameFilter = { true }): Sequence<KtClassifierSymbol>

    /**
     * Return a sequence of [KtClassifierSymbol] which current scope contains, if classifier name present in [names].
     *
     * The sequence includes: nested classes, inner classes, nested type aliases for the class scope,
     * and top-level classes and top-level type aliases for file scope.
     *
     * This implementation is more optimal than the one with `nameFilter` and should be used when the candidate name set is known.
     */
    public fun getClassifierSymbols(names: Collection<Name>): Sequence<KtClassifierSymbol>

    /**
     * Return a sequence of [KtClassifierSymbol] which current scope contains, if classifier name present in [names].
     *
     * @see getClassifierSymbols
     */
    public fun getClassifierSymbols(vararg names: Name): Sequence<KtClassifierSymbol> =
        getClassifierSymbols(names.toList())

    /**
     * Return a sequence of [KtConstructorSymbol] which current scope contain
     */
    public fun getConstructors(): Sequence<KtConstructorSymbol>
}

