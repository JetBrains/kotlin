/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dependenciesWithoutSelf
import org.jetbrains.kotlin.fir.resolve.AbstractFirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

open class FirDependenciesSymbolProviderImpl(val session: FirSession) : AbstractFirSymbolProvider<FirClassLikeSymbol<*>>() {
    protected open val dependencyProviders by lazy {
        val moduleInfo = session.moduleInfo ?: return@lazy emptyList()
        moduleInfo.dependenciesWithoutSelf().mapNotNull {
            session.sessionProvider?.getSession(it)?.firSymbolProvider
        }.toList()
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        return topLevelCallableCache.lookupCacheOrCalculate(CallableId(packageFqName, null, name)) {
            dependencyProviders.flatMap { provider -> provider.getTopLevelCallableSymbols(packageFqName, name) }
        } ?: emptyList()
    }

    override fun getNestedClassifierScope(classId: ClassId): FirScope? {
        return dependencyProviders.firstNotNullResult { it.getNestedClassifierScope(classId) }
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

    override fun getClassUseSiteMemberScope(
        classId: ClassId,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope? {
        return dependencyProviders.firstNotNullResult { it.getClassUseSiteMemberScope(classId, useSiteSession, scopeSession) }
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

    override fun getAllCallableNamesInPackage(fqName: FqName): Set<Name> {
        return dependencyProviders.flatMapTo(mutableSetOf()) { it.getAllCallableNamesInPackage(fqName) }
    }

    override fun getClassNamesInPackage(fqName: FqName): Set<Name> {
        return dependencyProviders.flatMapTo(mutableSetOf()) { it.getClassNamesInPackage(fqName) }
    }

    override fun getAllCallableNamesInClass(classId: ClassId): Set<Name> {
        return dependencyProviders.flatMapTo(mutableSetOf()) { it.getAllCallableNamesInClass(classId) }
    }

    override fun getNestedClassesNamesInClass(classId: ClassId): Set<Name> {
        return dependencyProviders.flatMapTo(mutableSetOf()) { it.getNestedClassesNamesInClass(classId) }
    }
}
