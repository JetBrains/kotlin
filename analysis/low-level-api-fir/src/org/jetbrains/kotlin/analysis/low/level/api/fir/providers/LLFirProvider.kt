/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.FirFileBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.ModuleFileCache
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProvider
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.originalForSubstitutionOverride
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@ThreadSafeMutableState
internal class LLFirProvider(
    @Suppress("UNUSED_PARAMETER") project: Project,
    val session: FirSession,
    @Suppress("UNUSED_PARAMETER") module: KtModule,
    val kotlinScopeProvider: FirKotlinScopeProvider,
    firFileBuilder: FirFileBuilder,
    val cache: ModuleFileCache,
    private val declarationProvider: KotlinDeclarationProvider,
    packageProvider: KotlinPackageProvider,
) : FirProvider() {
    override val symbolProvider: FirSymbolProvider = SymbolProvider()

    private val providerHelper = LLFirProviderHelper(
        cache,
        firFileBuilder,
        declarationProvider,
        packageProvider,
    )

    override val isPhasedFirAllowed: Boolean get() = true

    override fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration? =
        providerHelper.getFirClassifierByFqName(classId)

    override fun getFirClassifierContainerFile(fqName: ClassId): FirFile {
        return getFirClassifierContainerFileIfAny(fqName)
            ?: error("Couldn't find container for $fqName")
    }

    override fun getFirClassifierContainerFileIfAny(fqName: ClassId): FirFile? {
        val fir = getFirClassifierByFqName(fqName) ?: return null // Necessary to ensure cacheProvider contains this classifier
        return cache.getContainerFirFile(fir)
    }

    override fun getFirClassifierContainerFile(symbol: FirClassLikeSymbol<*>): FirFile {
        return getFirClassifierContainerFileIfAny(symbol)
            ?: error("Couldn't find container for ${symbol.classId}")
    }

    override fun getFirClassifierContainerFileIfAny(symbol: FirClassLikeSymbol<*>): FirFile? =
        cache.getContainerFirFile(symbol.fir)


    override fun getFirCallableContainerFile(symbol: FirCallableSymbol<*>): FirFile? {
        symbol.fir.originalForSubstitutionOverride?.symbol?.let {
            return getFirCallableContainerFile(it)
        }
        if (symbol is FirSyntheticPropertySymbol) {
            val fir = symbol.fir
            if (fir is FirSyntheticProperty) {
                return getFirCallableContainerFile(fir.getter.delegate.symbol)
            }
        }
        return cache.getContainerFirFile(symbol.fir)
    }

    override fun getFirFilesByPackage(fqName: FqName): List<FirFile> = error("Should not be called in FIR IDE")


    override fun getClassNamesInPackage(fqName: FqName): Set<Name> =
        declarationProvider.getClassNamesInPackage(fqName)

    override fun containsKotlinPackage(): Boolean = false

    @NoMutableState
    private inner class SymbolProvider : FirSymbolProvider(session) {
        override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> =
            providerHelper.getTopLevelCallableSymbols(packageFqName, name)

        @FirSymbolProviderInternals
        override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
            destination += getTopLevelCallableSymbols(packageFqName, name)
        }

        override fun getTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<FirNamedFunctionSymbol> =
            providerHelper.getTopLevelFunctionSymbols(packageFqName, name)

        @FirSymbolProviderInternals
        override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
            destination += getTopLevelFunctionSymbols(packageFqName, name)
        }

        override fun getTopLevelPropertySymbols(packageFqName: FqName, name: Name): List<FirPropertySymbol> =
            providerHelper.getTopLevelPropertySymbols(packageFqName, name)

        @FirSymbolProviderInternals
        override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
            destination += getTopLevelPropertySymbols(packageFqName, name)
        }

        override fun getPackage(fqName: FqName): FqName? =
            providerHelper.getPackage(fqName)

        override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
            return getFirClassifierByFqName(classId)?.symbol
        }
    }
}
