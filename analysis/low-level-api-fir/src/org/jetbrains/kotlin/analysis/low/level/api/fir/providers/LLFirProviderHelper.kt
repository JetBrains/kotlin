/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.getNotNullValueForNotNullContext
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirFileBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.LLFirResolveExtensionTool
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.llResolveExtensionTool
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirElementFinder
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLFirKotlinSymbolNamesProvider
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.createPackageProvider
import org.jetbrains.kotlin.analysis.providers.impl.declarationProviders.CompositeKotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.impl.packageProviders.CompositeKotlinPackageProvider
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.providers.FirCompositeCachedSymbolNamesProvider
import org.jetbrains.kotlin.fir.smartPlus
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withVirtualFileEntry

internal class LLFirProviderHelper(
    firSession: LLFirSession,
    private val firFileBuilder: LLFirFileBuilder,
    canContainKotlinPackage: Boolean,
    declarationProviderFactory: (GlobalSearchScope) -> KotlinDeclarationProvider?
) {
    private val extensionTool: LLFirResolveExtensionTool? = firSession.llResolveExtensionTool

    val searchScope: GlobalSearchScope =
        firSession.ktModule.contentScope.run {
            val notShadowedScope = extensionTool?.shadowedSearchScope?.let { GlobalSearchScope.notScope(it) }
            if (notShadowedScope != null) {
                this.intersectWith(notShadowedScope)
            } else {
                this
            }
        }

    val declarationProvider = CompositeKotlinDeclarationProvider.create(
        listOfNotNull(
            declarationProviderFactory(searchScope),
            extensionTool?.declarationProvider,
        )
    )

    val packageProvider = CompositeKotlinPackageProvider.create(
        listOfNotNull(
            firSession.project.createPackageProvider(searchScope),
            extensionTool?.packageProvider,
        )
    )

    val allowKotlinPackage: Boolean = canContainKotlinPackage ||
            firSession.languageVersionSettings.getFlag(AnalysisFlags.allowKotlinPackage)

    private val classifierByClassId: FirCache<ClassId, FirClassLikeDeclaration?, KtClassLikeDeclaration?> =
        firSession.firCachesFactory.createCache { classId, context ->
            computeClassifierByClassId(classId, context)
        }

    /**
     * Locates the [FirClassLikeDeclaration] with the matching [classId].
     * Uses the passed [context] files to avoid index access if available; falls back to the [declarationProvider] otherwise.
     *
     * To work correctly with the [FirCache], this function has to obey the following contract:
     *
     * It can be called with some [classId] and a non-null [context] **if and only if** the returned value
     * is going to be the same for the `null` context.
     */
    private fun computeClassifierByClassId(classId: ClassId, context: KtClassLikeDeclaration?): FirClassLikeDeclaration? {
        require(context == null || context.isPhysical)
        val ktClass = context ?: declarationProvider.getClassLikeDeclarationByClassId(classId) ?: return null

        if (ktClass.getClassId() == null) return null
        val firFile = firFileBuilder.buildRawFirFileWithCaching(ktClass.containingKtFile)
        return FirElementFinder.findClassifierWithClassId(firFile, classId)
            ?: errorWithAttachment("Classifier was found in KtFile but was not found in FirFile") {
                withEntry("classifierClassId", classId) { it.asString() }
                withVirtualFileEntry("virtualFile", ktClass.containingKtFile.virtualFile)
            }
    }

    private val functionsByCallableId: FirCache<CallableId, List<FirNamedFunctionSymbol>, Collection<KtFile>?> =
        firSession.firCachesFactory.createCache { callableId, context ->
            computeCallableSymbolsByCallableId<FirNamedFunctionSymbol>(callableId, context)
        }

    private val propertiesByCallableId: FirCache<CallableId, List<FirPropertySymbol>, Collection<KtFile>?> =
        firSession.firCachesFactory.createCache { callableId, context ->
            computeCallableSymbolsByCallableId<FirPropertySymbol>(callableId, context)
        }

    /**
     * Locates all the callable symbols of required [TYPE] with the matching [callableId] within a specific set of files.
     * Uses the passed [context] files to avoid index access if available; falls back to the [declarationProvider] otherwise.
     *
     * To work correctly with the [FirCache], this function has to obey the following contract:
     *
     * It can be called with some [callableId] and a non-null [context] **if and only if** the returned value
     * is going to be the same for the `null` context.
     */
    private inline fun <reified TYPE : FirCallableSymbol<*>> computeCallableSymbolsByCallableId(
        callableId: CallableId,
        context: Collection<KtFile>?,
    ): List<TYPE> {
        require(context == null || context.all { it.isPhysical })

        val files = if (context != null) {
            context
        } else {
            // we want to use `getTopLevelCallableFiles` instead of
            // `getTopLevelFunctions/Properties`, because it is highly optimized
            // to retrieve the files in the IDE mode
            declarationProvider.getTopLevelCallableFiles(callableId)
        }

        if (files.isEmpty()) return emptyList()

        return buildList {
            files.forEach { ktFile ->
                val firFile = firFileBuilder.buildRawFirFileWithCaching(ktFile)
                firFile.collectCallableSymbolsOfTypeTo<TYPE>(this, callableId.callableName)
            }
        }
    }

    val symbolNameCache = FirCompositeCachedSymbolNamesProvider.create(
        firSession,
        listOfNotNull(
            LLFirKotlinSymbolNamesProvider(declarationProvider, allowKotlinPackage),
            extensionTool?.symbolNamesProvider,
        )
    )

    /**
     * [classLikeDeclaration] is a [KtClassLikeDeclaration] which corresponds to the desired class.
     *
     * If already known, it can be provided to avoid index accesses.
     * But it has to be coherent with [KotlinDeclarationProvider.getClassLikeDeclarationByClassId]'s result,
     * see [computeClassifierByClassId].
     */
    fun getFirClassifierByFqNameAndDeclaration(
        classId: ClassId,
        classLikeDeclaration: KtClassLikeDeclaration?,
    ): FirClassLikeDeclaration? {
        if (classId.isLocal) return null
        if (!allowKotlinPackage && classId.isKotlinPackage()) return null
        return classifierByClassId.getNotNullValueForNotNullContext(classId, classLikeDeclaration)
    }

    fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        return getTopLevelCallableSymbols(CallableId(packageFqName, name), callableFiles = null)
    }

    /**
     * [callableFiles] are the [KtFile]s which contain callables of the given package and name.
     *
     * If already known, they can be provided to avoid index accesses.
     * But they have to be coherent with [KotlinDeclarationProvider.getTopLevelCallableFiles] content,
     * see [computeCallableSymbolsByCallableId].
     */
    fun getTopLevelCallableSymbols(callableId: CallableId, callableFiles: Collection<KtFile>?): List<FirCallableSymbol<*>> {
        if (!allowKotlinPackage && callableId.packageName.isKotlinPackage()) return emptyList()

        val functions = getTopLevelFunctionSymbols(callableId, callableFiles)
        val properties = getTopLevelPropertySymbols(callableId, callableFiles)

        return functions.smartPlus(properties)
    }

    fun getTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<FirNamedFunctionSymbol> {
        return getTopLevelFunctionSymbols(CallableId(packageFqName, name), callableFiles = null)
    }

    /**
     * [callableFiles] are the [KtFile]s which contain functions of the given package and name.
     *
     * If already known, they can be provided to avoid index accesses.
     * But they have to be coherent with [KotlinDeclarationProvider.getTopLevelFunctions] content,
     * see [computeCallableSymbolsByCallableId].
     */
    fun getTopLevelFunctionSymbols(callableId: CallableId, callableFiles: Collection<KtFile>?): List<FirNamedFunctionSymbol> {
        return functionsByCallableId.getValue(callableId, callableFiles)
    }

    fun getTopLevelPropertySymbols(packageFqName: FqName, name: Name): List<FirPropertySymbol> {
        return getTopLevelPropertySymbols(CallableId(packageFqName, name), callableFiles = null)
    }

    /**
     * [callableFiles] are the [KtFile]s which contain properties of the given package and name.
     *
     * If already known, they can be provided to avoid index accesses.
     * But they have to be coherent with [KotlinDeclarationProvider.getTopLevelProperties] content,
     * see [computeCallableSymbolsByCallableId].
     */
    fun getTopLevelPropertySymbols(callableId: CallableId, callableFiles: Collection<KtFile>?): List<FirPropertySymbol> {
        return propertiesByCallableId.getValue(callableId, callableFiles)
    }

    private inline fun <reified TYPE : FirCallableSymbol<*>> FirFile.collectCallableSymbolsOfTypeTo(list: MutableList<TYPE>, name: Name) {
        declarations.mapNotNullTo(list) { declaration ->
            if (declaration is FirCallableDeclaration && declaration.symbol.callableId.callableName == name) {
                declaration.symbol as? TYPE
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