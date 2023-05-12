/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.analysis.api.scopes.KtTypeScope
import org.jetbrains.kotlin.analysis.api.signatures.KtCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.name.Name

internal open class KtFirDelegatingTypeScope(
    val firScope: FirContainingNamesAwareScope,
    private val builder: KtSymbolByFirBuilder,
) : KtTypeScope {
    override val token: KtLifetimeToken get() = builder.token

    private val allNamesCached by cached {
        getPossibleCallableNames() + getPossibleClassifierNames()
    }

    override fun getAllPossibleNames(): Set<Name> = withValidityAssertion { allNamesCached }

    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        firScope.getCallableNames()
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        firScope.getClassifierNames()
    }

    override fun getCallableSignatures(nameFilter: KtScopeNameFilter): Sequence<KtCallableSignature<*>> = withValidityAssertion {
        firScope.getCallableSignatures(getPossibleCallableNames().filter(nameFilter), builder)
    }

    override fun getCallableSignatures(names: Collection<Name>): Sequence<KtCallableSignature<*>> = withValidityAssertion {
        firScope.getCallableSignatures(names, builder)
    }

    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> = withValidityAssertion {
        firScope.getClassifierSymbols(getPossibleClassifierNames().filter(nameFilter), builder)
    }

    override fun getClassifierSymbols(names: Collection<Name>): Sequence<KtClassifierSymbol> = withValidityAssertion {
        firScope.getClassifierSymbols(names, builder)
    }

    override fun getConstructors(): Sequence<KtConstructorSymbol> = withValidityAssertion {
        firScope.getConstructors(builder)
    }

    override fun mayContainName(name: Name): Boolean = withValidityAssertion {
        name in getAllPossibleNames()
    }
}