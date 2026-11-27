/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSessionBoundOwner
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class KaFirPackageScope(
    private val fqName: FqName,
    override val analysisSession: KaFirSession,
) : KaFirScope(), KaFirSessionBoundOwner {

    private val firScope: FirPackageMemberScope by lazy(LazyThreadSafetyMode.PUBLICATION) {
        FirPackageMemberScope(fqName, analysisSession.firSession)
    }

    override fun getPossibleCallableNames(): Set<Name> = entryPointImplementation {
        DeclarationsInPackageProvider.getTopLevelCallableNamesInPackageProvider(fqName, analysisSession = analysisSession)
    }

    override fun getPossibleClassifierNames(): Set<Name> = entryPointImplementation {
        DeclarationsInPackageProvider.getTopLevelClassifierNamesInPackageProvider(fqName, analysisSession = analysisSession)
    }

    override fun callables(nameFilter: (Name) -> Boolean): Sequence<KaCallableSymbol> = entryPointImplementation {
        firScope.getCallableSymbols(getPossibleCallableNames().filter(nameFilter))
    }

    override fun callables(names: Collection<Name>): Sequence<KaCallableSymbol> = entryPointImplementation {
        firScope.getCallableSymbols(names)
    }

    override fun classifiers(nameFilter: (Name) -> Boolean): Sequence<KaClassifierSymbol> = entryPointImplementation {
        firScope.getClassifierSymbols(getPossibleClassifierNames().filter(nameFilter))
    }

    override fun classifiers(names: Collection<Name>): Sequence<KaClassifierSymbol> = entryPointImplementation {
        firScope.getClassifierSymbols(names)
    }

    override val constructors: Sequence<KaConstructorSymbol>
        get() = entryPointImplementation { emptySequence() }

    override fun getPackageSymbols(nameFilter: (Name) -> Boolean): Sequence<KaPackageSymbol> = entryPointImplementation {
        sequence {
            analysisSession.useSitePackageProvider.getSubpackageNames(fqName, analysisSession.targetPlatform).forEach { name ->
                if (nameFilter(name)) {
                    yield(analysisSession.firSymbolBuilder.createPackageSymbol(fqName.child(name)))
                }
            }
        }
    }
}
