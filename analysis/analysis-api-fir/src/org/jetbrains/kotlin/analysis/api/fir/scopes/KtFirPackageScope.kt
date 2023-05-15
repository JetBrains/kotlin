/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class KtFirPackageScope(
    private val fqName: FqName,
    private val analysisSession: KtFirAnalysisSession,
) : KtScope {
    override val token: KtLifetimeToken get() = analysisSession.token

    private val firScope: FirPackageMemberScope by lazy(LazyThreadSafetyMode.PUBLICATION) {
        FirPackageMemberScope(fqName, analysisSession.useSiteSession)
    }

    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        DeclarationsInPackageProvider.getTopLevelCallableNamesInPackageProvider(fqName, analysisSession)
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        DeclarationsInPackageProvider.getTopLevelClassifierNamesInPackageProvider(fqName, analysisSession)
    }

    override fun getCallableSymbols(nameFilter: KtScopeNameFilter): Sequence<KtCallableSymbol> = withValidityAssertion {
        firScope.getCallableSymbols(getPossibleCallableNames().filter(nameFilter), analysisSession.firSymbolBuilder)
    }

    override fun getCallableSymbols(names: Collection<Name>): Sequence<KtCallableSymbol> = withValidityAssertion {
        firScope.getCallableSymbols(names, analysisSession.firSymbolBuilder)
    }

    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> = withValidityAssertion {
        firScope.getClassifierSymbols(getPossibleClassifierNames().filter(nameFilter), analysisSession.firSymbolBuilder)
    }

    override fun getClassifierSymbols(names: Collection<Name>): Sequence<KtClassifierSymbol> = withValidityAssertion {
        firScope.getClassifierSymbols(names, analysisSession.firSymbolBuilder)
    }

    override fun getConstructors(): Sequence<KtConstructorSymbol> = withValidityAssertion {
        emptySequence()
    }

    override fun getPackageSymbols(nameFilter: KtScopeNameFilter): Sequence<KtPackageSymbol> = withValidityAssertion {
        sequence {
            analysisSession.useSitePackageProvider.getSubPackageFqNames(fqName, analysisSession.targetPlatform, nameFilter).forEach {
                yield(analysisSession.firSymbolBuilder.createPackageSymbol(fqName.child(it)))
            }
        }
    }
}
