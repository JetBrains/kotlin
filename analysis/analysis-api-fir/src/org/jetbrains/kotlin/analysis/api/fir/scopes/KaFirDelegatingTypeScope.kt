/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaExperimentalApi::class)

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaTypeScope
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.name.Name

internal open class KaFirDelegatingTypeScope(
    val firScope: FirContainingNamesAwareScope,
    private val builder: KaSymbolByFirBuilder,
) : KaTypeScope {
    override val token: KaLifetimeToken get() = builder.token

    private val allNamesCached by cached {
        getPossibleCallableNames() + getPossibleClassifierNames()
    }

    @KaExperimentalApi
    override fun getAllPossibleNames(): Set<Name> = withValidityAssertion { allNamesCached }

    @KaExperimentalApi
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        firScope.getCallableNames()
    }

    @KaExperimentalApi
    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        firScope.getClassifierNames()
    }

    override fun getCallableSignatures(nameFilter: (Name) -> Boolean): Sequence<KaCallableSignature<*>> = withValidityAssertion {
        firScope.getCallableSignatures(getPossibleCallableNames().filter(nameFilter), builder)
    }

    override fun getCallableSignatures(names: Collection<Name>): Sequence<KaCallableSignature<*>> = withValidityAssertion {
        firScope.getCallableSignatures(names, builder)
    }

    override fun getClassifierSymbols(nameFilter: (Name) -> Boolean): Sequence<KaClassifierSymbol> = withValidityAssertion {
        firScope.getClassifierSymbols(getPossibleClassifierNames().filter(nameFilter), builder)
    }

    override fun getClassifierSymbols(names: Collection<Name>): Sequence<KaClassifierSymbol> = withValidityAssertion {
        firScope.getClassifierSymbols(names, builder)
    }

    override fun getConstructors(): Sequence<KaConstructorSymbol> = withValidityAssertion {
        firScope.getConstructors(builder)
    }

    @KaExperimentalApi
    override fun mayContainName(name: Name): Boolean = withValidityAssertion {
        name in getAllPossibleNames()
    }
}