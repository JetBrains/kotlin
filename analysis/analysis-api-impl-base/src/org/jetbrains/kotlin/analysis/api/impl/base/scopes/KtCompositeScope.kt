/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.scopes

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.name.Name

@KtAnalysisApiInternals
class KtCompositeScope private constructor(
    private val subScopes: List<KtScope>,
    override val token: KtLifetimeToken,
) : KtScope {

    init {
        require(subScopes.size > 1) {
            "Required `subScopes.size > 1` but `subScopes.size = ${subScopes.size}`"
        }
    }

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

    override fun getCallableSymbols(names: Collection<Name>): Sequence<KtCallableSymbol> = withValidityAssertion {
        if (names.isEmpty()) return emptySequence()
        sequence {
            subScopes.forEach { yieldAll(it.getCallableSymbols(names)) }
        }
    }

    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getClassifierSymbols(nameFilter)) }
        }
    }

    override fun getClassifierSymbols(names: Collection<Name>): Sequence<KtClassifierSymbol> = withValidityAssertion {
        if (names.isEmpty()) return emptySequence()
        sequence {
            subScopes.forEach { yieldAll(it.getClassifierSymbols(names)) }
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

    companion object {
        fun create(subScopes: List<KtScope>, token: KtLifetimeToken): KtScope =
            when (subScopes.size) {
                0 -> KtEmptyScope(token)
                1 -> subScopes.single()
                else -> KtCompositeScope(subScopes, token)
            }
    }
}