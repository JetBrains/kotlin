/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import org.jetbrains.kotlin.analysis.utils.collections.buildSmartList
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.utils.SmartSet

internal class LLFirModuleWithDependenciesSymbolProvider(
    session: FirSession,
    val providers: List<FirSymbolProvider>,
    val dependencyProvider: LLFirDependenciesSymbolProvider,
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
    fun getTopLevelCallableSymbolsWithoutDependencies(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        return buildList { getTopLevelCallableSymbolsToWithoutDependencies(this, packageFqName, name) }
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

    override fun computePackageSetWithTopLevelCallables(): Set<String>? = null
    override fun knownTopLevelClassifiersInPackage(packageFqName: FqName): Set<String>? = null
    override fun computeCallableNamesInPackage(packageFqName: FqName): Set<Name>? = null
}

internal class LLFirDependenciesSymbolProvider(
    session: FirSession,
    val providers: List<FirSymbolProvider>,
) : FirSymbolProvider(session) {
    init {
        require(providers.all { it !is LLFirModuleWithDependenciesSymbolProvider }) {
            "${LLFirDependenciesSymbolProvider::class.simpleName} may not contain ${LLFirModuleWithDependenciesSymbolProvider::class.simpleName}:" +
                    " dependency providers must be flattened during session creation."
        }
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? =
        providers.firstNotNullOfOrNull { it.getClassLikeSymbolByClassId(classId) }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        val facades = SmartSet.create<JvmClassName>()
        for (provider in providers) {
            val newSymbols = buildSmartList {
                provider.getTopLevelCallableSymbolsTo(this, packageFqName, name)
            }
            addNewSymbolsConsideringJvmFacades(destination, newSymbols, facades)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        val facades = SmartSet.create<JvmClassName>()
        for (provider in providers) {
            val newSymbols = buildSmartList {
                provider.getTopLevelFunctionSymbolsTo(this, packageFqName, name)
            }
            addNewSymbolsConsideringJvmFacades(destination, newSymbols, facades)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        val facades = SmartSet.create<JvmClassName>()
        for (provider in providers) {
            val newSymbols = buildSmartList {
                provider.getTopLevelPropertySymbolsTo(this, packageFqName, name)
            }
            addNewSymbolsConsideringJvmFacades(destination, newSymbols, facades)
        }
    }

    override fun getPackage(fqName: FqName): FqName? = providers.firstNotNullOfOrNull { it.getPackage(fqName) }

    override fun computePackageSetWithTopLevelCallables(): Set<String>? = null
    override fun knownTopLevelClassifiersInPackage(packageFqName: FqName): Set<String>? = null
    override fun computeCallableNamesInPackage(packageFqName: FqName): Set<Name>? = null

    private fun <S : FirCallableSymbol<*>> addNewSymbolsConsideringJvmFacades(
        destination: MutableList<S>,
        newSymbols: List<S>,
        facades: MutableSet<JvmClassName>,
    ) {
        if (newSymbols.isEmpty()) return
        val newFacades = SmartSet.create<JvmClassName>()
        for (symbol in newSymbols) {
            val facade = symbol.jvmClassName()
            if (facade != null) {
                newFacades += facade
                if (facade !in facades) {
                    destination += symbol
                }
            } else {
                destination += symbol
            }
        }
        facades += newFacades
    }

    private fun FirCallableSymbol<*>.jvmClassName(): JvmClassName? {
        val jvmPackagePartSource = fir.containerSource as? JvmPackagePartSource ?: return null
        return jvmPackagePartSource.facadeClassName ?: jvmPackagePartSource.className
    }
}
