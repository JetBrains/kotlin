/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.dependenciesWithoutSelf
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.resolve.providers.SymbolProviderCache
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@ThreadSafeMutableState
open class FirDependenciesSymbolProviderImpl(session: FirSession) : FirSymbolProvider(session) {
    private val classCache = SymbolProviderCache<ClassId, FirClassLikeSymbol<*>>()
    private val topLevelCallableCache = SymbolProviderCache<CallableId, List<FirCallableSymbol<*>>>()
    private val topLevelFunctionCache = SymbolProviderCache<CallableId, List<FirNamedFunctionSymbol>>()
    private val topLevelPropertyCache = SymbolProviderCache<CallableId, List<FirPropertySymbol>>()
    private val packageCache = SymbolProviderCache<FqName, FqName>()

    protected open val dependencyProviders by lazy {
        val moduleInfo = session.moduleInfo ?: return@lazy emptyList()
        moduleInfo.dependenciesWithoutSelf().mapNotNull {
            session.sessionProvider?.getSession(it)?.firSymbolProvider
        }.toList()
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        destination += topLevelFunctionCache.lookupCacheOrCalculate(CallableId(packageFqName, null, name)) {
            val result = mutableListOf<FirNamedFunctionSymbol>()
            dependencyProviders.forEach {
                it.getTopLevelFunctionSymbolsTo(result, packageFqName, name)
            }
            result
        } ?: emptyList()
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        destination += topLevelPropertyCache.lookupCacheOrCalculate(CallableId(packageFqName, null, name)) {
            val result = mutableListOf<FirPropertySymbol>()
            dependencyProviders.forEach {
                it.getTopLevelPropertySymbolsTo(result, packageFqName, name)
            }
            result
        } ?: emptyList()
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        destination += getTopLevelCallableSymbols(packageFqName, name)
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        return topLevelCallableCache.lookupCacheOrCalculate(CallableId(packageFqName, null, name)) {
            dependencyProviders.flatMap { provider -> provider.getTopLevelCallableSymbols(packageFqName, name) }
        } ?: emptyList()
    }

    override fun getClassLikeSymbolByFqName(classId: ClassId): FirClassLikeSymbol<*>? {
        return classCache.lookupCacheOrCalculate(classId) {
            for (provider in dependencyProviders) {
                provider.getClassLikeSymbolByFqName(classId)?.let {
                    return@lookupCacheOrCalculate it
                }
            }
            null
        }
    }

    override fun getPackage(fqName: FqName): FqName? {
        return packageCache.lookupCacheOrCalculate(fqName) {
            for (provider in dependencyProviders) {
                provider.getPackage(fqName)?.let {
                    return@lookupCacheOrCalculate it
                }
            }
            null
        }
    }
}
