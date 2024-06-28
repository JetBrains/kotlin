/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
 * A scope inside which use-site type-parameters of callable declarations may be substituted. Such declarations are represented as [KaCallableSignature].
 *
 * @see org.jetbrains.kotlin.analysis.api.components.KaScopeProviderMixIn.getTypeScope
 * @see KaCallableSignature
 */
@KaExperimentalApi
public interface KaTypeScope : KaScopeLike {

    /**
     * Return a sequence of [KaCallableSignature] which current scope contain if declaration name matches [nameFilter].
     */
    public fun getCallableSignatures(nameFilter: (Name) -> Boolean = { true }): Sequence<KaCallableSignature<*>>

    /**
     * Return a sequence of [KaCallableSignature] which current scope contain if declaration, if declaration name present in [names]
     *
     * This implementation is more optimal than the one with `nameFilter` and should be used when the candidate name set is known.
     */
    public fun getCallableSignatures(names: Collection<Name>): Sequence<KaCallableSignature<*>>

    /**
     * Return a sequence of [KaCallableSignature] which current scope contain if declaration, if declaration name present in [names]
     *
     * @see getCallableSignatures
     */
    public fun getCallableSignatures(vararg names: Name): Sequence<KaCallableSignature<*>> = withValidityAssertion {
        getCallableSignatures(names.toList())
    }

    /**
     * Return a sequence of [KaClassifierSymbol] which current scope contain if classifier name matches [nameFilter]. The sequence includes:
     * nested classes, inner classes, nested type aliases for the class scope, and top-level classes and top-level type aliases for file scope.
     *
     * This function needs to retrieve a set of all possible names before processing the scope.
     * The overload with `names: Collection<Name>` should be used when the candidate name set is known.
     */
    public fun getClassifierSymbols(nameFilter: (Name) -> Boolean = { true }): Sequence<KaClassifierSymbol>

    /**
     * Return a sequence of [KaClassifierSymbol] which current scope contains, if classifier name present in [names].
     *
     * The sequence includes: nested classes, inner classes, nested type aliases for the class scope,
     * and top-level classes and top-level type aliases for file scope.
     *
     * This implementation is more optimal than the one with `nameFilter` and should be used when the candidate name set is known.
     */
    public fun getClassifierSymbols(names: Collection<Name>): Sequence<KaClassifierSymbol>

    /**
     * Return a sequence of [KaClassifierSymbol] which current scope contains, if classifier name present in [names].
     *
     * @see getClassifierSymbols
     */
    public fun getClassifierSymbols(vararg names: Name): Sequence<KaClassifierSymbol> =
        getClassifierSymbols(names.toList())

    /**
     * Return a sequence of [KaConstructorSymbol] which current scope contain
     */
    public fun getConstructors(): Sequence<KaConstructorSymbol>
}

@KaExperimentalApi
@Deprecated("Use 'KaTypeScope' instead", ReplaceWith("KaTypeScope"))
public typealias KtTypeScope = KaTypeScope
