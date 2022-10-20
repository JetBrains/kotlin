/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.scopes

import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.name.Name

class KtCompositeScope(
    private val subScopes: List<KtScope>,
    override val token: KtLifetimeToken
) : KtScope {
    override fun getAllPossibleNames(): Set<Name> = withValidityAssertion {
        buildSet {
            subScopes.flatMapTo(this) { it.getAllPossibleNames() }
        }
    }

    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        buildSet {
            subScopes.flatMapTo(this) { it.getPossibleCallableNames() }
        }
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        buildSet {
            subScopes.flatMapTo(this) { it.getPossibleClassifierNames() }
        }
    }

    override fun getAllSymbols(): Sequence<KtDeclarationSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getAllSymbols()) }
        }
    }

    override fun getCallableSymbols(nameFilter: KtScopeNameFilter): Sequence<KtCallableSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getCallableSymbols(nameFilter)) }
        }
    }

    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getClassifierSymbols(nameFilter)) }
        }
    }

    override fun getConstructors(): Sequence<KtConstructorSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getConstructors()) }
        }
    }

    override fun getPackageSymbols(nameFilter: KtScopeNameFilter): Sequence<KtPackageSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getPackageSymbols(nameFilter)) }
        }
    }

    override fun mayContainName(name: Name): Boolean = withValidityAssertion {
        subScopes.any { it.mayContainName(name) }
    }
}