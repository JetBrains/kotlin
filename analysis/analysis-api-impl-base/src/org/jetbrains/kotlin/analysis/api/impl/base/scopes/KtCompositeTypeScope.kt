/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.scopes

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScopeNameFilter
import org.jetbrains.kotlin.analysis.api.scopes.KaTypeScope
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.name.Name

@KaAnalysisApiInternals
class KaCompositeTypeScope(
    val subScopes: List<KaTypeScope>,
    override val token: KaLifetimeToken
) : KaTypeScope {
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


    override fun getCallableSignatures(nameFilter: KaScopeNameFilter): Sequence<KaCallableSignature<*>> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getCallableSignatures(nameFilter)) }
        }
    }

    override fun getCallableSignatures(names: Collection<Name>): Sequence<KaCallableSignature<*>> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getCallableSignatures(names)) }
        }
    }

    override fun getClassifierSymbols(nameFilter: KaScopeNameFilter): Sequence<KaClassifierSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getClassifierSymbols(nameFilter)) }
        }
    }

    override fun getClassifierSymbols(names: Collection<Name>): Sequence<KaClassifierSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getClassifierSymbols(names)) }
        }
    }

    override fun getConstructors(): Sequence<KaConstructorSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getConstructors()) }
        }
    }

    override fun mayContainName(name: Name): Boolean = withValidityAssertion {
        subScopes.any { it.mayContainName(name) }
    }
}