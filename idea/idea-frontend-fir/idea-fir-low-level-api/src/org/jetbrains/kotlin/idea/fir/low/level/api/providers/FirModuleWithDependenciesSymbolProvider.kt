/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.providers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

internal class FirModuleWithDependenciesSymbolProvider(
    session: FirSession,
    private val providers: List<FirSymbolProvider>,
    private val dependentProviders: List<FirSymbolProvider>,
) : FirSymbolProvider(session) {

    override fun getClassLikeSymbolByFqName(classId: ClassId): FirClassLikeSymbol<*>? =
        providers.firstNotNullResult { it.getClassLikeSymbolByFqName(classId) }
            ?: withDependent(default = null) {
                dependentProviders.firstNotNullResult { it.getClassLikeSymbolByFqName(classId) }
            }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        providers.flatMapTo(destination) { it.getTopLevelCallableSymbols(packageFqName, name) }
        withDependent {
            dependentProviders.flatMapTo(destination) { it.getTopLevelCallableSymbols(packageFqName, name) }
        }
    }

    override fun getPackage(fqName: FqName): FqName? =
        providers.firstNotNullResult { it.getPackage(fqName) }
            ?: withDependent(default = null) {
                dependentProviders.firstNotNullResult { it.getPackage(fqName) }
            }

    companion object {
        private val canUseDependent = ThreadLocal.withInitial { true }

        private inline fun <R> withDependent(default: R, action: () -> R): R {
            if (canUseDependent.get()) {
                canUseDependent.set(false)
                try {
                    return action()
                } finally {
                    canUseDependent.set(true)
                }
            }
            return default
        }

        private inline fun withDependent(action: () -> Unit) {
            withDependent(Unit, action)
        }
    }
}
