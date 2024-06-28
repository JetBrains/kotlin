/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirCompositeSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
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
internal class LLFirCombinedSyntheticFunctionSymbolProvider private constructor(
    session: FirSession,
    private val providers: List<FirSyntheticFunctionInterfaceProviderBase>,
) : FirSymbolProvider(session) {
    private val combinedPackageNames: Set<FqName> = providers.flatMapTo(mutableSetOf()) { it.getFunctionKindPackageNames() }

    override val symbolNamesProvider: FirSymbolNamesProvider =
        // `FirCompositeSymbolNamesProvider` defines `mayHaveSyntheticFunctionTypes` and `mayHaveSyntheticFunctionType` correctly, which is
        // needed for consistency should this symbol provider be part of another composite symbol provider.
        object : FirCompositeSymbolNamesProvider(providers.map { it.symbolNamesProvider }) {
            override fun getPackageNames(): Set<String> = emptySet()

            override val hasSpecificClassifierPackageNamesComputation: Boolean get() = false
            override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name> = emptySet()

            override val hasSpecificCallablePackageNamesComputation: Boolean get() = false
            override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> = emptySet()
        }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        if (!classId.mayBeSyntheticFunctionClassName()) return null
        if (classId.packageFqName !in combinedPackageNames) return null

        return providers.firstNotNullOfOrNull { it.getClassLikeSymbolByClassId(classId) }
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

    companion object {
        fun merge(session: FirSession, providers: List<FirSyntheticFunctionInterfaceProviderBase>): FirSymbolProvider? =
            if (providers.size > 1) LLFirCombinedSyntheticFunctionSymbolProvider(session, providers)
            else providers.singleOrNull()
    }
}
