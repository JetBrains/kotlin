/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.resolve.calls.overloads.ConeEquivalentCallConflictResolver
import org.jetbrains.kotlin.fir.resolve.providers.FirCompositeSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * This is the special symbol provider for support of the hierarchical MPP compilation scheme.
 * In this scheme, each module in the HMPP hierarchy depends on its own dependencies: metadata klibs for common modules,
 * platform jars/klibs for the leaf platform module.
 *
 * In the HMPP compilation scheme, common modules might refer to declarations from common metadata klibs, which later
 * should be actualized after the FIR2IR conversion. So this provider is needed to collect the mapping of such declarations.
 *
 * It works in the following way: for each request the provider lookups both in the platform and common dependencies
 * and stores the common/platform mapping in case if the declaration(s) was found in both providers.
 */
@OptIn(SymbolInternals::class)
class FirCommonDeclarationsMappingSymbolProvider(
    session: FirSession,
    val commonSymbolProvider: FirSymbolProvider,
    val platformSymbolProvider: FirSymbolProvider,
) : FirSymbolProvider(session) {
    private val providers: List<FirSymbolProvider> = listOf(commonSymbolProvider, platformSymbolProvider)

    data class ClassPair(val commonClass: FirClassLikeSymbol<*>?, val platformClass: FirClassLikeSymbol<*>?)

    val classMapping: Map<ClassId, ClassPair>
        field = mutableMapOf()

    private val processedCallables: MutableMap<CallableId, List<FirCallableSymbol<*>>> = hashMapOf()

    val commonCallableToPlatformCallableMap: Map<FirCallableSymbol<*>, FirCallableSymbol<*>>
        field = mutableMapOf()

    override val symbolNamesProvider: FirSymbolNamesProvider = FirCompositeSymbolNamesProvider.Companion.fromSymbolProviders(providers)

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        val commonSymbol = commonSymbolProvider.getClassLikeSymbolByClassId(classId)
        val platformSymbol = platformSymbolProvider.getClassLikeSymbolByClassId(classId)

        if (commonSymbol == null && platformSymbol == null) return null

        classMapping[classId] = ClassPair(commonSymbol, platformSymbol)

        return when {
            commonSymbol == null -> platformSymbol
            platformSymbol == null -> commonSymbol
            commonSymbol == platformSymbol -> commonSymbol
            else -> platformSymbol
        }
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        val callableId = CallableId(packageFqName, name)
        processedCallables[callableId]?.let { return it }

        val commonDeclarations = commonSymbolProvider.getTopLevelCallableSymbols(packageFqName, name)
        val platformDeclarations = platformSymbolProvider.getTopLevelCallableSymbols(packageFqName, name)
        val resultingDeclarations = preferPlatformDeclarations(commonDeclarations, platformDeclarations)
        processedCallables[callableId] = resultingDeclarations
        return resultingDeclarations
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        destination += getTopLevelCallableSymbols(packageFqName, name)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        getTopLevelCallableSymbols(packageFqName, name).filterIsInstanceTo<FirNamedFunctionSymbol, _>(destination)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        getTopLevelCallableSymbols(packageFqName, name).filterIsInstanceTo<FirPropertySymbol, _>(destination)
    }

    override fun hasPackage(fqName: FqName): Boolean {
        return providers.any { it.hasPackage(fqName) }
    }

    private fun <D : FirCallableDeclaration, S : FirCallableSymbol<D>> preferPlatformDeclarations(
        commonDeclarations: List<S>,
        platformDeclarations: List<S>,
    ): List<S> {
        val result = platformDeclarations.toMutableList()

        for (commonSymbol in commonDeclarations) {
            val matchingPlatformSymbol = platformDeclarations.firstOrNull { areEquivalentTopLevelCallables(it.fir, commonSymbol.fir) }

            if (matchingPlatformSymbol != null) {
                commonCallableToPlatformCallableMap[commonSymbol] = matchingPlatformSymbol
            } else {
                result += commonSymbol
            }
        }
        return result
    }

    private fun areEquivalentTopLevelCallables(
        first: FirCallableDeclaration,
        second: FirCallableDeclaration,
    ): Boolean {
        return ConeEquivalentCallConflictResolver.areEquivalentTopLevelCallables(
            first,
            second,
            session,
            argumentMappingIsEqual = null
        )
    }
}
