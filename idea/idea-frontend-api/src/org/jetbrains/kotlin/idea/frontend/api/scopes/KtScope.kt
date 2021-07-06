/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.scopes

import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithDeclarations
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name


public interface KtScope : ValidityTokenOwner {
    /**
     * Returns a **subset** of names which current scope may contain.
     * In other words `ALL_NAMES(scope)` is a subset of `scope.getAllNames()`
     */
    public fun getAllPossibleNames(): Set<Name> = withValidityAssertion {
        getPossibleCallableNames() + getPossibleClassifierNames()
    }

    /**
     * Returns a **subset** of callable names which current scope may contain.
     * In other words `ALL_CALLABLE_NAMES(scope)` is a subset of `scope.getCallableNames()`
     */
    public fun getPossibleCallableNames(): Set<Name>

    /**
     * Returns a **subset** of classifier names which current scope may contain.
     * In other words `ALL_CLASSIFIER_NAMES(scope)` is a subset of `scope.getClassifierNames()`
     */
    public fun getPossibleClassifierNames(): Set<Name>


    /**
     * Return a sequence of all [KtSymbol] which current scope contain
     */
    public fun getAllSymbols(): Sequence<KtSymbol> = withValidityAssertion {
        sequence {
            yieldAll(getCallableSymbols())
            yieldAll(getClassifierSymbols())
            yieldAll(getConstructors())
        }
    }

    /**
     * Return a sequence of [KtCallableSymbol] which current scope contain if declaration name matches [nameFilter]
     */
    public fun getCallableSymbols(nameFilter: KtScopeNameFilter = { true }): Sequence<KtCallableSymbol>

    /**
     * Return a sequence of [KtClassifierSymbol] which current scope contain if declaration name matches [nameFilter]
     */
    public fun getClassifierSymbols(nameFilter: KtScopeNameFilter = { true }): Sequence<KtClassifierSymbol>

    /**
     * Return a sequence of [KtConstructorSymbol] which current scope contain
     */
    public fun getConstructors(): Sequence<KtConstructorSymbol>

    /**
     * return true if the scope may contain name, false otherwise.
     *
     * In other words `(mayContainName(name) == false) => (name !in scope)`; vice versa is not always true
     */
    public fun mayContainName(name: Name): Boolean = withValidityAssertion {
        name in getPossibleCallableNames() || name in getPossibleClassifierNames()
    }
}

public typealias KtScopeNameFilter = (Name) -> Boolean

public interface KtCompositeScope : KtScope {
    public val subScopes: List<KtScope>
}

public interface KtMemberScope : KtDeclarationScope<KtSymbolWithMembers> {
    override val owner: KtSymbolWithMembers
}

public interface KtDeclaredMemberScope : KtDeclarationScope<KtSymbolWithMembers> {
    override val owner: KtSymbolWithMembers
}

public interface KtDeclarationScope<out T : KtSymbolWithDeclarations> : KtScope {
    public val owner: T
}

public interface KtPackageScope : KtScope {
    public val fqName: FqName

    public fun getPackageSymbols(nameFilter: KtScopeNameFilter = { true }): Sequence<KtPackageSymbol>

    override fun getAllSymbols(): Sequence<KtSymbol> = withValidityAssertion {
        super.getAllSymbols() + getPackageSymbols()
    }

    override fun getConstructors(): Sequence<KtConstructorSymbol> = withValidityAssertion { emptySequence() }
}