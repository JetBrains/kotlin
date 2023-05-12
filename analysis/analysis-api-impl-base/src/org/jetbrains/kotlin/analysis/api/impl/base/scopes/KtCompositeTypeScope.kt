/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.scopes

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.analysis.api.scopes.KtTypeScope
import org.jetbrains.kotlin.analysis.api.signatures.KtCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.name.Name

@KtAnalysisApiInternals
class KtCompositeTypeScope(
    val subScopes: List<KtTypeScope>,
    override val token: KtLifetimeToken
) : KtTypeScope {
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


    override fun getCallableSignatures(nameFilter: KtScopeNameFilter): Sequence<KtCallableSignature<*>> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getCallableSignatures(nameFilter)) }
        }
    }

    override fun getCallableSignatures(names: Collection<Name>): Sequence<KtCallableSignature<*>>  = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getCallableSignatures(names)) }
        }
    }

    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getClassifierSymbols(nameFilter)) }
        }
    }

    override fun getClassifierSymbols(names: Collection<Name>): Sequence<KtClassifierSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getClassifierSymbols(names)) }
        }
    }

    override fun getConstructors(): Sequence<KtConstructorSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getConstructors()) }
        }
    }

    override fun mayContainName(name: Name): Boolean = withValidityAssertion {
        subScopes.any { it.mayContainName(name) }
    }
}