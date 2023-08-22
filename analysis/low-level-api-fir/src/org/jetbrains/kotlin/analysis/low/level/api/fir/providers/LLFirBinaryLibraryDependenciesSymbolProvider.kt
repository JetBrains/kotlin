/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionCache
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class LLFirBinaryLibraryDependenciesSymbolProvider(
    val symbolProvider: LLFirModuleWithDependenciesSymbolProvider,
    val module: KtLibraryModule,
    session: FirSession
) : FirSymbolProvider(session) {
    val llFirSessionCache = LLFirSessionCache.getInstance(module.project)
    val dependencies = buildSet {
        addAll(module.directRegularDependencies)
        addAll(module.transitiveDependsOnDependencies)
    }
    override val symbolNamesProvider: FirSymbolNamesProvider = FirNullSymbolNamesProvider

    @OptIn(UnsafeCastFunction::class)
    private fun <T> withVisited (
        visitedModules: MutableSet<KtLibraryModule>,
        body: FirSymbolProvider.() -> T,
    ): T? {
        if (module in visitedModules)
            return null
        visitedModules.add(module)
        dependencies.minus(visitedModules).forEach {
            val moduleSymbolProvider = llFirSessionCache.getSession(it, true).symbolProvider
            val symbol = when(it) {
                is KtLibraryModule -> {
                    moduleSymbolProvider
                        .safeAs<LLFirBinaryLibraryDependenciesSymbolProvider>()?.run {
                            symbolProvider.body() ?: withVisited(visitedModules, body)
                        }
                }
                else -> {
                    moduleSymbolProvider.body()
                }
            }
            if (symbol != null)
                return symbol
        }
        return null
    }

    @OptIn(UnsafeCastFunction::class)
    private fun withVisitedVoid(
        visitedModules: MutableSet<KtLibraryModule>,
        body: FirSymbolProvider.() -> Unit,
    ) {
        if (module in visitedModules)
            return
        visitedModules.add(module)
        dependencies.minus(visitedModules).forEach {
            val moduleSymbolProvider = llFirSessionCache.getSession(it, true).symbolProvider
            when (it) {
                is KtLibraryModule -> {
                    moduleSymbolProvider
                        .safeAs<LLFirBinaryLibraryDependenciesSymbolProvider>()?.run {
                            symbolProvider.body()
                            withVisited(visitedModules, body)
                        }

                }
                else -> {
                    moduleSymbolProvider.body()
                }
            }
        }
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        return symbolProvider.getClassLikeSymbolByClassId(classId) ?: withVisited(mutableSetOf()) {
            getClassLikeSymbolByClassId(classId)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<FirCallableSymbol<*>>,
        packageFqName: FqName,
        name: Name,
    ) {
        symbolProvider.getTopLevelCallableSymbolsTo(destination, packageFqName, name)
        withVisitedVoid(mutableSetOf()) {
            getTopLevelCallableSymbolsTo(destination, packageFqName, name)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<FirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
        symbolProvider.getTopLevelFunctionSymbolsTo(destination, packageFqName, name)
        withVisitedVoid(mutableSetOf()) {
            getTopLevelFunctionSymbolsTo(destination, packageFqName, name)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        symbolProvider.getTopLevelPropertySymbolsTo(destination, packageFqName, name)
        withVisitedVoid(mutableSetOf()) {
            getTopLevelPropertySymbolsTo(destination, packageFqName, name)
        }
    }

    override fun getPackage(fqName: FqName): FqName? {
        return symbolProvider.getPackage(fqName) ?: withVisited(mutableSetOf()) {
            getPackage(fqName)
        }
    }

}