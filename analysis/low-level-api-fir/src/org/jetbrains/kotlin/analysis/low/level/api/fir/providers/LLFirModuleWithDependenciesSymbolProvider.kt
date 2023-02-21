/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProviderByKtModule
import org.jetbrains.kotlin.analysis.utils.collections.buildSmartList
import org.jetbrains.kotlin.fir.extensions.FirSwitchableExtensionDeclarationsSymbolProvider
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.OptionalAnnotationClassesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirSyntheticFunctionInterfaceProviderBase
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
    session: LLFirSession,
    val dependencyProvider: LLFirDependentModuleProviders,
    val mainProvider: FirSymbolProvider,
    val additionalProviders: List<FirSymbolProvider>,
) : FirSymbolProvider(session) {

    private val storage by lazy {
        LLFirCallablesStorage(session.project, listOf(session))
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? =
        getClassLikeSymbolByFqNameWithoutDependencies(classId)
            ?: dependencyProvider.getClassLikeSymbolByClassId(classId)


    fun getClassLikeSymbolByFqNameWithoutDependencies(classId: ClassId): FirClassLikeSymbol<*>? =
        mainProvider.getClassLikeSymbolByClassId(classId)
            ?: additionalProviders.firstNotNullOfOrNull { it.getClassLikeSymbolByClassId(classId) }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        getTopLevelCallableSymbolsToWithoutDependencies(destination, packageFqName, name)
        dependencyProvider.getTopLevelCallableSymbolsTo(destination, packageFqName, name)
    }

    @FirSymbolProviderInternals
    fun getTopLevelCallableSymbolsToWithoutDependencies(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        storage.getProvidersByPackage(packageFqName).forEach { it.getTopLevelCallableSymbolsTo(destination, packageFqName, name) }
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
        storage.getProvidersByPackage(packageFqName).forEach { it.getTopLevelFunctionSymbolsTo(destination, packageFqName, name) }
    }

    @FirSymbolProviderInternals
    fun getTopLevelPropertySymbolsToWithoutDependencies(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        storage.getProvidersByPackage(packageFqName).forEach { it.getTopLevelPropertySymbolsTo(destination, packageFqName, name) }
    }

    override fun getPackage(fqName: FqName): FqName? =
        getPackageWithoutDependencies(fqName)
            ?: dependencyProvider.getPackage(fqName)

    override fun computePackageSetWithTopLevelCallables(): Set<String>? = null

    override fun knownTopLevelClassifiersInPackage(packageFqName: FqName): Set<String>? = null

    override fun computeCallableNamesInPackage(packageFqName: FqName): Set<Name>? = null


    fun getPackageWithoutDependencies(fqName: FqName): FqName? =
        mainProvider.getPackage(fqName)
            ?: additionalProviders.firstNotNullOfOrNull { it.getPackage(fqName) }
}

internal abstract class LLFirDependentModuleProviders(
    session: LLFirSession,
) : FirSymbolProvider(session) {

    abstract val dependentProviders: List<FirSymbolProvider>
    abstract val dependentSessions: List<LLFirSession>

    private val storage by lazy {
        LLFirCallablesStorage(session.project, dependentSessions)
    }

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
        for (provider in storage.getProvidersByPackage(packageFqName)) {
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
        for (provider in storage.getProvidersByPackage(packageFqName)) {
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
        for (provider in storage.getProvidersByPackage(packageFqName)) {
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

    // TODO: Consider having proper implementations for sake of optimizations
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

internal class LLFirDependentModuleProvidersBySessions(
    session: LLFirSession,
    override val dependentSessions: List<LLFirSession>
) : LLFirDependentModuleProviders(session) {

    override val dependentProviders: List<FirSymbolProvider> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        dependentSessions.map { it.symbolProvider }
    }

    constructor(session: LLFirSession, createSessions: MutableList<LLFirSession>.() -> Unit)
            : this(session, buildList { createSessions() })
}


internal class LLFirDependentModuleProvidersByProviders(
    session: LLFirSession,
    override val dependentProviders: List<FirSymbolProvider>,
) : LLFirDependentModuleProviders(session) {

    override val dependentSessions: List<LLFirSession>
        get() = dependentProviders.map { it.session as LLFirSession }

    constructor(session: LLFirSession, createProviders: MutableList<FirSymbolProvider>.() -> Unit)
            : this(session, buildList { createProviders() })
}

internal class LLFirCallablesStorage(project: Project, sessions: List<LLFirSession>) {
    private val moduleByPackageWithCallables: Map<FqName, List<FirSymbolProvider>> = run {
        val packageProvider = KotlinPackageProviderByKtModule.getInstance(project)
        buildMap {
            for (session in sessions) {
                collectMappings(session.symbolProvider, packageProvider)
            }
        }.mapValues { it.value.toList() }
    }

    fun getProvidersByPackage(packageFqName: FqName): List<FirSymbolProvider> =
        moduleByPackageWithCallables[packageFqName].orEmpty()

    private fun MutableMap<FqName, MutableSet<FirSymbolProvider>>.collectMappings(
        symbolProvider: FirSymbolProvider,
        packageProvider: KotlinPackageProviderByKtModule
    ) {
        when (symbolProvider) {
            is LLFirModuleWithDependenciesSymbolProvider -> {
                val session = symbolProvider.session as LLFirSession
                val ktModule = session.ktModule
                for (packageFqName in packageProvider.getContainedPackages(ktModule)) {
                    getOrPut(packageFqName) { mutableSetOf() }.add(symbolProvider.mainProvider)
                }
                for (additionalProvider in symbolProvider.additionalProviders) {
                    collectMappings(additionalProvider, packageProvider)
                }
            }
            is JavaSymbolProvider -> {
                // no top-level callables
            }
            is FirSyntheticFunctionInterfaceProviderBase -> {
                // no top-level callables
            }
            is OptionalAnnotationClassesProvider -> {
                // no top-level callables
            }
            is FirCloneableSymbolProvider -> {
                // no top-level callables
            }
            is FirSwitchableExtensionDeclarationsSymbolProvider -> {
                // TODO support later
            }
            else -> {
                error("Unknown $symbolProvider")
            }
        }
    }
}