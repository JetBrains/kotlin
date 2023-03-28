/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirFileBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirElementFinder
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLFirKotlinSymbolProviderNameCache
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProvider
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtFile

internal class LLFirProviderHelper(
    firSession: FirSession,
    private val firFileBuilder: LLFirFileBuilder,
    private val declarationProvider: KotlinDeclarationProvider,
    private val packageProvider: KotlinPackageProvider,
    canContainKotlinPackage: Boolean,
) {
    private val allowKotlinPackage = canContainKotlinPackage ||
            firSession.languageVersionSettings.getFlag(AnalysisFlags.allowKotlinPackage)

    private val classifierByClassId =
        firSession.firCachesFactory.createCache<ClassId, FirClassLikeDeclaration?, KtClassLikeDeclaration?> { classId, context ->
            val ktClass = context ?: declarationProvider.getClassLikeDeclarationByClassId(classId) ?: return@createCache null

            if (ktClass.getClassId() == null) return@createCache null
            val firFile = firFileBuilder.buildRawFirFileWithCaching(ktClass.containingKtFile)
            FirElementFinder.findClassifierWithClassId(firFile, classId)
                ?: error("Classifier $classId was found in file ${ktClass.containingKtFile.virtualFilePath} but was not found in FirFile")
        }

    private val callablesByCallableId =
        firSession.firCachesFactory.createCache<CallableId, List<FirCallableSymbol<*>>, Collection<KtFile>?> { callableId, context ->
            val files = context ?: declarationProvider.getTopLevelCallableFiles(callableId).ifEmpty { return@createCache emptyList() }
            buildList {
                files.forEach { ktFile ->
                    val firFile = firFileBuilder.buildRawFirFileWithCaching(ktFile)
                    firFile.collectCallableDeclarationsTo(this, callableId.callableName)
                }
            }
        }

    val symbolNameCache = LLFirKotlinSymbolProviderNameCache(firSession, declarationProvider)

    fun getFirClassifierByFqNameAndDeclaration(
        classId: ClassId,
        classLikeDeclaration: KtClassLikeDeclaration?,
    ): FirClassLikeDeclaration? {
        if (classId.isLocal) return null
        if (!allowKotlinPackage && classId.isKotlinPackage()) return null
        return classifierByClassId.getValue(classId, classLikeDeclaration)
    }

    fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        if (!allowKotlinPackage && packageFqName.isKotlinPackage()) return emptyList()
        val callableId = CallableId(packageFqName, name)
        return callablesByCallableId.getValue(callableId)
    }

    /**
     * [callableFiles] are the [KtFile]s which contain callables of the given package and name. If already known, they can be provided to
     * avoid index accesses.
     */
    fun getTopLevelCallableSymbols(callableId: CallableId, callableFiles: Collection<KtFile>?): List<FirCallableSymbol<*>> {
        if (!allowKotlinPackage && callableId.packageName.isKotlinPackage()) return emptyList()
        return callablesByCallableId.getValue(callableId, callableFiles)
    }

    fun getTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<FirNamedFunctionSymbol> {
        return getTopLevelCallableSymbols(packageFqName, name).filterIsInstance<FirNamedFunctionSymbol>()
    }

    fun getTopLevelFunctionSymbols(callableId: CallableId, callableFiles: Collection<KtFile>): List<FirNamedFunctionSymbol> {
        return getTopLevelCallableSymbols(callableId, callableFiles).filterIsInstance<FirNamedFunctionSymbol>()
    }

    fun getTopLevelPropertySymbols(packageFqName: FqName, name: Name): List<FirPropertySymbol> {
        return getTopLevelCallableSymbols(packageFqName, name).filterIsInstance<FirPropertySymbol>()
    }

    fun getTopLevelPropertySymbols(callableId: CallableId, callableFiles: Collection<KtFile>): List<FirPropertySymbol> {
        return getTopLevelCallableSymbols(callableId, callableFiles).filterIsInstance<FirPropertySymbol>()
    }

    private fun FirFile.collectCallableDeclarationsTo(list: MutableList<FirCallableSymbol<*>>, name: Name) {
        declarations.mapNotNullTo(list) { declaration ->
            if (declaration is FirCallableDeclaration && declaration.symbol.callableId.callableName == name) {
                declaration.symbol
            } else null
        }
    }

    fun getPackage(fqName: FqName): FqName? {
        if (!allowKotlinPackage && fqName.isKotlinPackage()) return null
        return fqName.takeIf(packageProvider::doesKotlinOnlyPackageExist)
    }
}

private fun ClassId.isKotlinPackage(): Boolean = startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)
private fun FqName.isKotlinPackage(): Boolean = startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)