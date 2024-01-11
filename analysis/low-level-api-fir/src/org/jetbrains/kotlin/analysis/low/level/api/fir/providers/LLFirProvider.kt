/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProvider
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

@ThreadSafeMutableState
internal class LLFirProvider(
    val session: LLFirSession,
    private val moduleComponents: LLFirModuleResolveComponents,
    canContainKotlinPackage: Boolean,
    disregardSelfDeclarations: Boolean = false,
    declarationProviderFactory: (GlobalSearchScope) -> KotlinDeclarationProvider?,
) : FirProvider() {
    override val symbolProvider: FirSymbolProvider =
        if (disregardSelfDeclarations) LLEmptySymbolProvider(session) else SymbolProvider()

    private val providerHelper = LLFirProviderHelper(
        session,
        moduleComponents.firFileBuilder,
        canContainKotlinPackage,
        declarationProviderFactory,
    )

    val searchScope: GlobalSearchScope
        get() = providerHelper.searchScope

    override val isPhasedFirAllowed: Boolean get() = true

    override fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration? =
        getFirClassifierByFqNameAndDeclaration(classId, classLikeDeclaration = null)

    fun getFirClassifierByDeclaration(classLikeDeclaration: KtClassLikeDeclaration): FirClassLikeDeclaration? {
        val classId = classLikeDeclaration.getClassId() ?: return null
        return getFirClassifierByFqNameAndDeclaration(classId, classLikeDeclaration)
    }

    private fun getFirClassifierByFqNameAndDeclaration(
        classId: ClassId,
        classLikeDeclaration: KtClassLikeDeclaration?,
    ): FirClassLikeDeclaration? {
        return providerHelper.getFirClassifierByFqNameAndDeclaration(classId, classLikeDeclaration)
    }

    override fun getFirClassifierContainerFile(fqName: ClassId): FirFile {
        return getFirClassifierContainerFileIfAny(fqName)
            ?: errorWithAttachment("Couldn't find container") {
                withEntry("classId", fqName.asString())
            }
    }

    override fun getFirClassifierContainerFileIfAny(fqName: ClassId): FirFile? {
        return getFirClassifierByFqName(fqName)?.let { moduleComponents.cache.getContainerFirFile(it) }
    }

    override fun getFirClassifierContainerFile(symbol: FirClassLikeSymbol<*>): FirFile {
        return getFirClassifierContainerFileIfAny(symbol)
            ?: errorWithAttachment("Couldn't find container") {
                withFirSymbolEntry("symbol", symbol)
            }
    }

    override fun getFirClassifierContainerFileIfAny(symbol: FirClassLikeSymbol<*>): FirFile? {
        return moduleComponents.cache.getContainerFirFile(symbol.fir)
    }

    override fun getFirCallableContainerFile(symbol: FirCallableSymbol<*>): FirFile? {
        return moduleComponents.cache.getContainerFirFile(symbol.fir)
    }

    override fun getFirScriptContainerFile(symbol: FirScriptSymbol): FirFile? {
        return moduleComponents.cache.getContainerFirFile(symbol.fir)
    }

    // TODO: implement
    override fun getFirScriptByFilePath(path: String): FirScriptSymbol? = null

    override fun getFirFilesByPackage(fqName: FqName): List<FirFile> = error("Should not be called in FIR IDE")

    override fun getClassNamesInPackage(fqName: FqName): Set<Name> =
        providerHelper.symbolNameCache.getTopLevelClassifierNamesInPackage(fqName)
            ?: errorWithAttachment("Cannot compute the set of class names in the given package") {
                withEntry("packageFqName", fqName.asString())
            }

    @NoMutableState
    internal inner class SymbolProvider : LLFirKotlinSymbolProvider(session) {
        override val declarationProvider: KotlinDeclarationProvider get() = providerHelper.declarationProvider

        override val packageProvider: KotlinPackageProvider get() = providerHelper.packageProvider

        override val symbolNamesProvider: FirSymbolNamesProvider get() = providerHelper.symbolNameCache

        override val allowKotlinPackage get() = providerHelper.allowKotlinPackage

        override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
            if (!providerHelper.symbolNameCache.mayHaveTopLevelClassifier(classId)) return null
            return getFirClassifierByFqName(classId)?.symbol
        }

        @FirSymbolProviderInternals
        override fun getClassLikeSymbolByClassId(classId: ClassId, classLikeDeclaration: KtClassLikeDeclaration): FirClassLikeSymbol<*>? {
            return getFirClassifierByFqNameAndDeclaration(classId, classLikeDeclaration)?.symbol
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

        override fun getPackage(fqName: FqName): FqName? =
            providerHelper.getPackage(fqName)
    }
}
