/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirProviderHelper
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

/**
 * [LLKotlinSourceSymbolProvider] is a [LLKotlinSymbolProvider] which provides symbols for source-based modules, such as [KaSourceModule][org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule]
 * and [KaScriptModule][org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule].
 */
internal class LLKotlinSourceSymbolProvider(
    session: LLFirSession,
    moduleComponents: LLFirModuleResolveComponents,
    searchScope: GlobalSearchScope,
    canContainKotlinPackage: Boolean,
    declarationProviderFactory: (GlobalSearchScope) -> KotlinDeclarationProvider?,
) : LLKotlinSymbolProvider(session) {
    private val providerHelper = LLFirProviderHelper(
        session,
        moduleComponents.firFileBuilder,
        searchScope,
        canContainKotlinPackage,
        declarationProviderFactory,
    )

    override val declarationProvider: KotlinDeclarationProvider get() = providerHelper.declarationProvider

    override val packageProvider: KotlinPackageProvider get() = providerHelper.packageProvider

    override val symbolNamesProvider: FirSymbolNamesProvider get() = providerHelper.symbolNameCache

    override val allowKotlinPackage get() = providerHelper.allowKotlinPackage

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        if (!providerHelper.symbolNameCache.mayHaveTopLevelClassifier(classId)) return null
        return providerHelper.getFirClassifierByFqNameAndDeclaration(classId, classLikeDeclaration = null)?.symbol
    }

    @FirSymbolProviderInternals
    override fun getClassLikeSymbolByClassId(classId: ClassId, classLikeDeclaration: KtClassLikeDeclaration): FirClassLikeSymbol<*>? {
        return providerHelper.getFirClassifierByFqNameAndDeclaration(classId, classLikeDeclaration)?.symbol
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        if (!providerHelper.symbolNameCache.mayHaveTopLevelCallable(packageFqName, name)) return emptyList()
        return providerHelper.getTopLevelCallableSymbols(packageFqName, name)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        if (!providerHelper.symbolNameCache.mayHaveTopLevelCallable(packageFqName, name)) return
        destination += providerHelper.getTopLevelCallableSymbols(packageFqName, name)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<FirCallableSymbol<*>>,
        callableId: CallableId,
        callables: Collection<KtCallableDeclaration>
    ) {
        destination += providerHelper.getTopLevelCallableSymbols(callableId, callables.mapTo(mutableSetOf()) { it.containingKtFile })
    }

    override fun getTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<FirNamedFunctionSymbol> {
        if (!providerHelper.symbolNameCache.mayHaveTopLevelCallable(packageFqName, name)) return emptyList()
        return providerHelper.getTopLevelFunctionSymbols(packageFqName, name)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        if (!providerHelper.symbolNameCache.mayHaveTopLevelCallable(packageFqName, name)) return
        destination += providerHelper.getTopLevelFunctionSymbols(packageFqName, name)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<FirNamedFunctionSymbol>,
        callableId: CallableId,
        functions: Collection<KtNamedFunction>
    ) {
        destination += providerHelper.getTopLevelFunctionSymbols(callableId, functions.mapTo(mutableSetOf()) { it.containingKtFile })
    }

    override fun getTopLevelPropertySymbols(packageFqName: FqName, name: Name): List<FirPropertySymbol> {
        if (!providerHelper.symbolNameCache.mayHaveTopLevelCallable(packageFqName, name)) return emptyList()
        return providerHelper.getTopLevelPropertySymbols(packageFqName, name)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        if (!providerHelper.symbolNameCache.mayHaveTopLevelCallable(packageFqName, name)) return
        destination += providerHelper.getTopLevelPropertySymbols(packageFqName, name)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<FirPropertySymbol>,
        callableId: CallableId,
        properties: Collection<KtProperty>
    ) {
        destination += providerHelper.getTopLevelPropertySymbols(callableId, properties.mapTo(mutableSetOf()) { it.containingKtFile })
    }

    override fun hasPackage(fqName: FqName): Boolean =
        providerHelper.hasPackage(fqName)
}
