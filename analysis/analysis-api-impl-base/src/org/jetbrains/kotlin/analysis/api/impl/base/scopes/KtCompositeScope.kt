/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.scopes

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.scopes.KaScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.name.Name

@KaAnalysisApiInternals
class KaCompositeScope private constructor(
    private val subScopes: List<KaScope>,
    override val token: KaLifetimeToken,
) : KaScope {

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

    override fun getAllSymbols(): Sequence<KaDeclarationSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getAllSymbols()) }
        }
    }

    override fun getCallableSymbols(nameFilter: KaScopeNameFilter): Sequence<KaCallableSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getCallableSymbols(nameFilter)) }
        }
    }

    override fun getCallableSymbols(names: Collection<Name>): Sequence<KaCallableSymbol> = withValidityAssertion {
        if (names.isEmpty()) return emptySequence()
        sequence {
            subScopes.forEach { yieldAll(it.getCallableSymbols(names)) }
        }
    }

    override fun getClassifierSymbols(nameFilter: KaScopeNameFilter): Sequence<KaClassifierSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getClassifierSymbols(nameFilter)) }
        }
    }

    override fun getClassifierSymbols(names: Collection<Name>): Sequence<KaClassifierSymbol> = withValidityAssertion {
        if (names.isEmpty()) return emptySequence()
        sequence {
            subScopes.forEach { yieldAll(it.getClassifierSymbols(names)) }
        }
    }

    override fun getConstructors(): Sequence<KaConstructorSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getConstructors()) }
        }
    }

    override fun getPackageSymbols(nameFilter: KaScopeNameFilter): Sequence<KaPackageSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getPackageSymbols(nameFilter)) }
        }
    }

    override fun mayContainName(name: Name): Boolean = withValidityAssertion {
        subScopes.any { it.mayContainName(name) }
    }

    companion object {
        fun create(subScopes: List<KaScope>, token: KaLifetimeToken): KaScope =
            when (subScopes.size) {
                0 -> KaEmptyScope(token)
                1 -> subScopes.single()
                else -> KaCompositeScope(subScopes, token)
            }
    }
}