/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@RequiresOptIn
annotation class FirSymbolProviderInternals

abstract class FirSymbolProvider(val session: FirSession) : FirSessionComponent {
    abstract fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>?

    @OptIn(FirSymbolProviderInternals::class)
    open fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        return buildList { getTopLevelCallableSymbolsTo(this, packageFqName, name) }
    }

    @FirSymbolProviderInternals
    abstract fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name)

    @OptIn(FirSymbolProviderInternals::class)
    open fun getTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<FirNamedFunctionSymbol> {
        return buildList { getTopLevelFunctionSymbolsTo(this, packageFqName, name) }
    }

    @FirSymbolProviderInternals
    abstract fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name)

    @OptIn(FirSymbolProviderInternals::class)
    open fun getTopLevelPropertySymbols(packageFqName: FqName, name: Name): List<FirPropertySymbol> {
        return buildList { getTopLevelPropertySymbolsTo(this, packageFqName, name) }
    }

    @FirSymbolProviderInternals
    abstract fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name)

    abstract fun getPackage(fqName: FqName): FqName? // TODO: Replace to symbol sometime
}

abstract class FirDependenciesSymbolProvider(session: FirSession) : FirSymbolProvider(session)




val FirSession.symbolProvider: FirSymbolProvider by FirSession.sessionComponentAccessor()
val FirSession.dependenciesSymbolProvider: FirSymbolProvider by FirSession.sessionComponentAccessor<FirDependenciesSymbolProvider>()
