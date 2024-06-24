/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.scopes

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
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

    @KaExperimentalApi
    override fun getAllPossibleNames(): Set<Name> = withValidityAssertion {
        buildSet {
            subScopes.flatMapTo(this) { it.getAllPossibleNames() }
        }
    }

    @KaExperimentalApi
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        buildSet {
            subScopes.flatMapTo(this) { it.getPossibleCallableNames() }
        }
    }

    @KaExperimentalApi
    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        buildSet {
            subScopes.flatMapTo(this) { it.getPossibleClassifierNames() }
        }
    }

    override val declarations: Sequence<KaDeclarationSymbol>
        get() = withValidityAssertion {
            sequence {
                subScopes.forEach { yieldAll(it.declarations) }
            }
        }

    override fun callables(nameFilter: (Name) -> Boolean): Sequence<KaCallableSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.callables(nameFilter)) }
        }
    }

    override fun callables(names: Collection<Name>): Sequence<KaCallableSymbol> = withValidityAssertion {
        if (names.isEmpty()) return emptySequence()
        sequence {
            subScopes.forEach { yieldAll(it.callables(names)) }
        }
    }

    override fun classifiers(nameFilter: (Name) -> Boolean): Sequence<KaClassifierSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.classifiers(nameFilter)) }
        }
    }

    override fun classifiers(names: Collection<Name>): Sequence<KaClassifierSymbol> = withValidityAssertion {
        if (names.isEmpty()) return emptySequence()
        sequence {
            subScopes.forEach { yieldAll(it.classifiers(names)) }
        }
    }

    override val constructors: Sequence<KaConstructorSymbol>
        get() = withValidityAssertion {
            sequence {
                subScopes.forEach { yieldAll(it.constructors) }
            }
        }

    @KaExperimentalApi
    override fun getPackageSymbols(nameFilter: (Name) -> Boolean): Sequence<KaPackageSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getPackageSymbols(nameFilter)) }
        }
    }

    @KaExperimentalApi
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
