/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dependenciesWithoutSelf
import org.jetbrains.kotlin.fir.resolve.AbstractFirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class FirDependenciesSymbolProviderImpl(val session: FirSession) : AbstractFirSymbolProvider() {
    private val dependencyProviders by lazy {
        val moduleInfo = session.moduleInfo ?: return@lazy emptyList()
        moduleInfo.dependenciesWithoutSelf().mapNotNull {
            session.sessionProvider?.getSession(it)?.service<FirSymbolProvider>()
        }.toList()
    }

    override fun getCallableSymbols(callableId: CallableId): List<ConeCallableSymbol> {
        return callableCache.lookupCacheOrCalculate(callableId) {
            for (provider in dependencyProviders) {
                provider.getCallableSymbols(callableId).let {
                    if (it.isNotEmpty()) return@lookupCacheOrCalculate it
                }
            }
            emptyList()
        }.orEmpty()
    }

    override fun getClassLikeSymbolByFqName(classId: ClassId): ConeClassLikeSymbol? {
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