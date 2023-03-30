/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirSyntheticFunctionInterfaceProviderBase
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirSyntheticFunctionInterfaceProviderBase.Companion.mayBeSyntheticFunctionClassName
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * [LLFirCombinedSyntheticFunctionSymbolProvider] combines multiple synthetic function symbol providers with the advantage that [ClassId]
 * heuristics are checked only once.
 */
@OptIn(FirSymbolProviderInternals::class)
internal class LLFirCombinedSyntheticFunctionSymbolProvider(
    session: FirSession,
    private val providers: List<FirSyntheticFunctionInterfaceProviderBase>,
) : FirSymbolProvider(session) {
    private val combinedPackageNames: Set<FqName> = providers.flatMapTo(mutableSetOf()) { it.getFunctionKindPackageNames() }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        if (!classId.mayBeSyntheticFunctionClassName()) return null
        if (classId.packageFqName !in combinedPackageNames) return null

        return providers.firstNotNullOfOrNull { it.getClassLikeSymbolByClassIdWithoutClassIdChecks(classId) }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
    }

    override fun getPackage(fqName: FqName): FqName? = fqName.takeIf { it in combinedPackageNames }

    override fun computePackageSetWithTopLevelCallables(): Set<String>? = null
    override fun knownTopLevelClassifiersInPackage(packageFqName: FqName): Set<String>? = null
    override fun computeCallableNamesInPackage(packageFqName: FqName): Set<Name>? = null

    companion object {
        fun merge(session: FirSession, providers: List<FirSyntheticFunctionInterfaceProviderBase>): FirSymbolProvider? =
            if (providers.size > 1) LLFirCombinedSyntheticFunctionSymbolProvider(session, providers)
            else providers.singleOrNull()
    }
}
