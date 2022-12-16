/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.utils.collections.buildSmartList
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirDependenciesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
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

    override fun computePackageSet(): Set<String> {
        TODO("Not yet implemented")
    }

    override fun mayHaveTopLevelClass(classId: ClassId): Boolean {
        TODO("Not yet implemented")
    }

    override fun computeCallableNames(fqName: FqName): Set<Name>? {
        TODO("Not yet implemented")
    }

    override fun knownTopLevelClassifiers(fqName: FqName): Set<String> {
        TODO("Not yet implemented")
    }
}

internal abstract class LLFirDependentModuleProviders(
    session: FirSession,
) : FirDependenciesSymbolProvider(session) {

    abstract val dependentProviders: List<FirSymbolProvider>
    abstract val dependentSessions: List<LLFirSession>

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? =
        dependentProviders.firstNotNullOfOrNull { provider ->
            when (provider) {
                is LLFirModuleWithDependenciesSymbolProvider -> provider.getClassLikeSymbolByFqNameWithoutDependencies(classId)
                else -> provider.getClassLikeSymbolByClassId(classId)
            }
        }


    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        val facades = SmartSet.create<JvmClassName>()
        for (provider in dependentProviders) {
            val newSymbols = buildSmartList {
                when (provider) {
                    is LLFirModuleWithDependenciesSymbolProvider -> provider.getTopLevelCallableSymbolsToWithoutDependencies(this, packageFqName, name)
                    else -> provider.getTopLevelCallableSymbolsTo(this, packageFqName, name)
                }
            }
            addNewSymbolsConsideringJvmFacades(destination, newSymbols, facades)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        val facades = SmartSet.create<JvmClassName>()
        for (provider in dependentProviders) {
            val newSymbols = buildSmartList {
                when (provider) {
                    is LLFirModuleWithDependenciesSymbolProvider -> provider.getTopLevelFunctionSymbolsToWithoutDependencies(this, packageFqName, name)
                    else -> provider.getTopLevelFunctionSymbolsTo(this, packageFqName, name)
                }
            }

            addNewSymbolsConsideringJvmFacades(destination, newSymbols, facades)
        }

    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        val facades = SmartSet.create<JvmClassName>()
        for (provider in dependentProviders) {
            val newSymbols = buildSmartList {
                when (provider) {
                    is LLFirModuleWithDependenciesSymbolProvider -> provider.getTopLevelPropertySymbolsToWithoutDependencies(this, packageFqName, name)
                    else -> provider.getTopLevelPropertySymbolsTo(this, packageFqName, name)
                }
            }
            addNewSymbolsConsideringJvmFacades(destination, newSymbols, facades)
        }
    }

    override fun getPackage(fqName: FqName): FqName? =
        dependentProviders.firstNotNullOfOrNull { provider ->
            when (provider) {
                is LLFirModuleWithDependenciesSymbolProvider -> provider.getPackageWithoutDependencies(fqName)
                else -> provider.getPackage(fqName)
            }
        }


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

    override fun computePackageSet(): Set<String> {
        TODO("Not yet implemented")
    }

    override fun mayHaveTopLevelClass(classId: ClassId): Boolean {
        TODO("Not yet implemented")
    }

    override fun computeCallableNames(fqName: FqName): Set<Name>? {
        TODO("Not yet implemented")
    }

    override fun knownTopLevelClassifiers(fqName: FqName): Set<String> {
        TODO("Not yet implemented")
    }
}

internal class LLFirDependentModuleProvidersBySessions(
    session: FirSession,
    override val dependentSessions: List<LLFirSession>
) : LLFirDependentModuleProviders(session) {

    override val dependentProviders: List<FirSymbolProvider> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        dependentSessions.map { it.symbolProvider }
    }

    constructor(session: FirSession, createSessions: MutableList<LLFirSession>.() -> Unit)
            : this(session, buildList { createSessions() })

    override fun knownTopLevelClassifiers(fqName: FqName): Set<String> {
        TODO("Not yet implemented")
    }
}


internal class LLFirDependentModuleProvidersByProviders(
    session: FirSession,
    override val dependentProviders: List<FirSymbolProvider>,
) : LLFirDependentModuleProviders(session) {

    override val dependentSessions: List<LLFirSession>
        get() = dependentProviders.map { it.session as LLFirSession }

    constructor(session: FirSession, createProviders: MutableList<FirSymbolProvider>.() -> Unit)
            : this(session, buildList { createProviders() })
}
