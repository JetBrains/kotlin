/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaExperimentalApi::class)

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class KaFirPackageScope(
    private val fqName: FqName,
    private val analysisSession: KaFirSession,
) : KaScope {
    override val token: KaLifetimeToken get() = analysisSession.token

    private val firScope: FirPackageMemberScope by lazy(LazyThreadSafetyMode.PUBLICATION) {
        FirPackageMemberScope(fqName, analysisSession.firSession)
    }

    @KaExperimentalApi
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        DeclarationsInPackageProvider.getTopLevelCallableNamesInPackageProvider(fqName, analysisSession)
    }

    @KaExperimentalApi
    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        DeclarationsInPackageProvider.getTopLevelClassifierNamesInPackageProvider(fqName, analysisSession)
    }

    override fun callables(nameFilter: (Name) -> Boolean): Sequence<KaCallableSymbol> = withValidityAssertion {
        firScope.getCallableSymbols(getPossibleCallableNames().filter(nameFilter), analysisSession.firSymbolBuilder)
    }

    override fun callables(names: Collection<Name>): Sequence<KaCallableSymbol> = withValidityAssertion {
        firScope.getCallableSymbols(names, analysisSession.firSymbolBuilder)
    }

    override fun classifiers(nameFilter: (Name) -> Boolean): Sequence<KaClassifierSymbol> = withValidityAssertion {
        firScope.getClassifierSymbols(getPossibleClassifierNames().filter(nameFilter), analysisSession.firSymbolBuilder)
    }

    override fun classifiers(names: Collection<Name>): Sequence<KaClassifierSymbol> = withValidityAssertion {
        firScope.getClassifierSymbols(names, analysisSession.firSymbolBuilder)
    }

    override val constructors: Sequence<KaConstructorSymbol>
        get() = withValidityAssertion { emptySequence() }

    @KaExperimentalApi
    override fun getPackageSymbols(nameFilter: (Name) -> Boolean): Sequence<KaPackageSymbol> = withValidityAssertion {
        sequence {
            analysisSession.useSitePackageProvider.getSubPackageFqNames(fqName, analysisSession.targetPlatform, nameFilter).forEach {
                yield(analysisSession.firSymbolBuilder.createPackageSymbol(fqName.child(it)))
            }
        }
    }
}
