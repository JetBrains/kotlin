/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.scopes

import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

interface KtScope : ValidityTokenOwner {
    // TODO check that names are accessible
    // maybe return some kind of lazy set
    fun getAllNames(): Set<Name> = withValidityAssertion { getCallableNames() + getClassifierNames() }

    fun getCallableNames(): Set<Name>
    fun getClassifierNames(): Set<Name>


    fun getAllSymbols(): Sequence<KtSymbol> = withValidityAssertion {
        sequence {
            yieldAll(getCallableSymbols())
            yieldAll(getClassifierSymbols())
        }
    }

    fun getCallableSymbols(nameFilter: KtScopeNameFilter = { true }): Sequence<KtCallableSymbol>
    fun getClassifierSymbols(nameFilter: KtScopeNameFilter = { true }): Sequence<KtClassifierSymbol>

    fun containsName(name: Name): Boolean = withValidityAssertion {
        name in getCallableNames() || name in getClassifierNames()
    }
}

typealias KtScopeNameFilter = (Name) -> Boolean

interface KtCompositeScope : KtScope {
    val subScopes: List<KtScope>
}

interface KtMemberScope : KtScope {
    val owner: KtClassOrObjectSymbol
}

interface KtDeclaredMemberScope : KtScope {
    val owner: KtClassOrObjectSymbol
}

interface KtPackageScope : KtScope, KtSubstitutedScope<KtPackageScope> {
    val fqName: FqName
}

interface KtUnsubstitutedScope<S : KtScope> : KtScope {
    fun substitute(/*substitution*/): KtSubstitutedScope<S> = TODO()
}

interface KtSubstitutedScope<S> : KtScope