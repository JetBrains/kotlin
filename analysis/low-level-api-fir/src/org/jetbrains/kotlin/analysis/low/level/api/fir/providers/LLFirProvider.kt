/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.SyntheticFirClassProvider
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
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtFile

@ThreadSafeMutableState
internal class LLFirProvider(
    val session: FirSession,
    private val moduleComponents: LLFirModuleResolveComponents,
    private val declarationProvider: KotlinDeclarationProvider,
    packageProvider: KotlinPackageProvider,
    canContainKotlinPackage: Boolean,
) : FirProvider() {
    override val symbolProvider: FirSymbolProvider = SymbolProvider()

    private val providerHelper = LLFirProviderHelper(
        session,
        moduleComponents.firFileBuilder,
        declarationProvider,
        packageProvider,
        canContainKotlinPackage,
    )

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
    ): FirClassLikeDeclaration? = SyntheticFirClassProvider.getInstance(session).getFirClassifierByFqName(classId)
        ?: providerHelper.getFirClassifierByFqNameAndDeclaration(classId, classLikeDeclaration)

    override fun getFirClassifierContainerFile(fqName: ClassId): FirFile {
        return getFirClassifierContainerFileIfAny(fqName)
            ?: error("Couldn't find container for $fqName")
    }

    override fun getFirClassifierContainerFileIfAny(fqName: ClassId): FirFile? {
        return SyntheticFirClassProvider.getInstance(session).getFirClassifierContainerFileIfAny(fqName)
            ?: getFirClassifierByFqName(fqName)?.let { moduleComponents.cache.getContainerFirFile(it) }
    }

    override fun getFirClassifierContainerFile(symbol: FirClassLikeSymbol<*>): FirFile {
        return getFirClassifierContainerFileIfAny(symbol)
            ?: error("Couldn't find container for ${symbol.classId}")
    }

    override fun getFirClassifierContainerFileIfAny(symbol: FirClassLikeSymbol<*>): FirFile? {
        return SyntheticFirClassProvider.getInstance(session).getFirClassifierContainerFileIfAny(symbol.classId)
            ?: moduleComponents.cache.getContainerFirFile(symbol.fir)
    }

    override fun getFirCallableContainerFile(symbol: FirCallableSymbol<*>): FirFile? {
        symbol.fir.originalForSubstitutionOverride?.symbol?.let { originalSymbol ->
            return originalSymbol.moduleData.session.firProvider.getFirCallableContainerFile(originalSymbol)
        }

        val fir = symbol.fir
        return when {
            symbol is FirBackingFieldSymbol -> getFirCallableContainerFile(symbol.fir.propertySymbol)
            symbol is FirSyntheticPropertySymbol && fir is FirSyntheticProperty -> getFirCallableContainerFile(fir.getter.delegate.symbol)
            else -> {
                symbol.callableId.classId?.let { SyntheticFirClassProvider.getInstance(session).getFirClassifierContainerFileIfAny(it) }
                    ?: moduleComponents.cache.getContainerFirFile(symbol.fir)
            }
        }
    }

    override fun getFirScriptContainerFile(symbol: FirScriptSymbol): FirFile? {
        return moduleComponents.cache.getContainerFirFile(symbol.fir)
    }

    override fun getFirFilesByPackage(fqName: FqName): List<FirFile> = error("Should not be called in FIR IDE")


    override fun getClassNamesInPackage(fqName: FqName): Set<Name> =
        declarationProvider.getTopLevelKotlinClassLikeDeclarationNamesInPackage(fqName)

    @NoMutableState
    internal inner class SymbolProvider : FirSymbolProvider(session) {
        override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
            if (!providerHelper.symbolNameCache.mayHaveTopLevelClassifier(classId, mayHaveFunctionClass = false)) return null
            return getFirClassifierByFqName(classId)?.symbol
        }

        /**
         * This function is optimized for a known [classLikeDeclaration].
         */
        @FirSymbolProviderInternals
        fun getClassLikeSymbolByClassId(classId: ClassId, classLikeDeclaration: KtClassLikeDeclaration): FirClassLikeSymbol<*>? {
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

        /**
         * This function is optimized for known [callableFiles], which should be the list of all [KtFile]s that contain the callables.
         */
        @FirSymbolProviderInternals
        fun getTopLevelCallableSymbolsTo(
            destination: MutableList<FirCallableSymbol<*>>,
            callableId: CallableId,
            callableFiles: Collection<KtFile>,
        ) {
            destination += providerHelper.getTopLevelCallableSymbols(callableId, callableFiles)
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

        /**
         * This function is optimized for known [callableFiles], which should be the list of all [KtFile]s that contain the functions.
         */
        @FirSymbolProviderInternals
        fun getTopLevelFunctionSymbolsTo(
            destination: MutableList<FirNamedFunctionSymbol>,
            callableId: CallableId,
            callableFiles: Collection<KtFile>,
        ) {
            destination += providerHelper.getTopLevelFunctionSymbols(callableId, callableFiles)
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

        /**
         * This function is optimized for known [callableFiles], which should be the list of all [KtFile]s that contain the properties.
         */
        @FirSymbolProviderInternals
        fun getTopLevelPropertySymbolsTo(
            destination: MutableList<FirPropertySymbol>,
            callableId: CallableId,
            callableFiles: Collection<KtFile>,
        ) {
            destination += providerHelper.getTopLevelPropertySymbols(callableId, callableFiles)
        }

        override fun getPackage(fqName: FqName): FqName? =
            providerHelper.getPackage(fqName)

        // Computing the set of such package names is expensive and would require a new index. For now, it is not worth the marginal gains.
        override fun computePackageSetWithTopLevelCallables(): Set<String>? = null

        override fun knownTopLevelClassifiersInPackage(packageFqName: FqName): Set<String>? =
            providerHelper.symbolNameCache.getTopLevelClassifierNamesInPackage(packageFqName)

        override fun computeCallableNamesInPackage(packageFqName: FqName): Set<Name>? =
            providerHelper.symbolNameCache.getTopLevelCallableNamesInPackage(packageFqName)
    }
}
