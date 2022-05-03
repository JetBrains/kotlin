/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirDependenciesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class LLFirModuleWithDependenciesSymbolProvider(
    session: FirSession,
    val dependencyProvider: LLFirDependentModuleProviders,
    private val providers: List<FirSymbolProvider>,
) : FirSymbolProvider(session) {

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? =
        getClassLikeSymbolByFqNameWithoutDependencies(classId)
            ?: dependencyProvider.getClassLikeSymbolByClassId(classId)


    fun getClassLikeSymbolByFqNameWithoutDependencies(classId: ClassId): FirClassLikeSymbol<*>? =
        providers.firstNotNullOfOrNull { it.getClassLikeSymbolByClassId(classId) }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        getTopLevelCallableSymbolsToWithoutDependencies(destination, packageFqName, name)
        dependencyProvider.getTopLevelCallableSymbolsTo(destination, packageFqName, name)
    }

    @FirSymbolProviderInternals
    fun getTopLevelCallableSymbolsToWithoutDependencies(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        providers.forEach { it.getTopLevelCallableSymbolsTo(destination, packageFqName, name) }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        getTopLevelFunctionSymbolsToWithoutDependencies(destination, packageFqName, name)
        dependencyProvider.getTopLevelFunctionSymbolsTo(destination, packageFqName, name)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        getTopLevelPropertySymbolsToWithoutDependencies(destination, packageFqName, name)
        dependencyProvider.getTopLevelPropertySymbolsTo(destination, packageFqName, name)
    }

    @FirSymbolProviderInternals
    fun getTopLevelFunctionSymbolsToWithoutDependencies(
        destination: MutableList<FirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name
    ) {
        providers.forEach { it.getTopLevelFunctionSymbolsTo(destination, packageFqName, name) }
    }

    @FirSymbolProviderInternals
    fun getTopLevelPropertySymbolsToWithoutDependencies(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        providers.forEach { it.getTopLevelPropertySymbolsTo(destination, packageFqName, name) }
    }

    override fun getPackage(fqName: FqName): FqName? =
        getPackageWithoutDependencies(fqName)
            ?: dependencyProvider.getPackage(fqName)


    fun getPackageWithoutDependencies(fqName: FqName): FqName? =
        providers.firstNotNullOfOrNull { it.getPackage(fqName) }
}

internal class LLFirDependentModuleProviders(
    session: FirSession,
    private val providers: List<FirSymbolProvider>
) : FirDependenciesSymbolProvider(session) {

    constructor(session: FirSession, createSubProviders: MutableList<FirSymbolProvider>.() -> Unit)
            : this(session, buildList { createSubProviders() })

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? =
        providers.firstNotNullOfOrNull { provider ->
            when (provider) {
                is LLFirModuleWithDependenciesSymbolProvider -> provider.getClassLikeSymbolByFqNameWithoutDependencies(classId)
                else -> provider.getClassLikeSymbolByClassId(classId)
            }
        }


    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        providers.forEach { provider ->
            when (provider) {
                is LLFirModuleWithDependenciesSymbolProvider ->
                    provider.getTopLevelCallableSymbolsToWithoutDependencies(destination, packageFqName, name)
                else -> provider.getTopLevelCallableSymbolsTo(destination, packageFqName, name)
            }
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        providers.forEach { provider ->
            when (provider) {
                is LLFirModuleWithDependenciesSymbolProvider ->
                    provider.getTopLevelFunctionSymbolsToWithoutDependencies(destination, packageFqName, name)
                else -> provider.getTopLevelFunctionSymbolsTo(destination, packageFqName, name)
            }
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        providers.forEach { provider ->
            when (provider) {
                is LLFirModuleWithDependenciesSymbolProvider ->
                    provider.getTopLevelPropertySymbolsToWithoutDependencies(destination, packageFqName, name)
                else -> provider.getTopLevelPropertySymbolsTo(destination, packageFqName, name)
            }
        }
    }

    override fun getPackage(fqName: FqName): FqName? =
        providers.firstNotNullOfOrNull { provider ->
            when (provider) {
                is LLFirModuleWithDependenciesSymbolProvider -> provider.getPackageWithoutDependencies(fqName)
                else -> provider.getPackage(fqName)
            }
        }
}
