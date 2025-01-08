/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinCompositeDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinCompositePackageProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.createPackageProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.getNotNullValueForNotNullContext
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.LLFirResolveExtensionTool
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.llResolveExtensionTool
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirElementFinder
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLFirKotlinSymbolNamesProvider
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.providers.FirCompositeCachedSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.smartPlus
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withVirtualFileEntry

/**
 * [LLKotlinSourceSymbolProvider] is a [LLKotlinSymbolProvider] which provides symbols for source-based modules, such as [KaSourceModule][org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule]
 * and [KaScriptModule][org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule].
 *
 * ### Resolve extension symbols
 *
 * The symbol provider includes symbols from [LLFirResolveExtensionTool]s. While it would be nicer to have a separate resolve extension
 * symbol provider, such a setup would destroy the classpath order once symbol providers are combined into
 * [LLCombinedKotlinSymbolProvider][org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.combined.LLCombinedKotlinSymbolProvider]s,
 * because we can only ever combine one symbol provider per module. This means that we would combine all regular Kotlin source symbol
 * providers and append resolve extension symbol providers to the end, which changes the classpath order, as all resolve extensions are now
 * queried last. So a source module which occurs later in the dependencies would be able to shadow a symbol from an earlier resolve
 * extension.
 *
 * Even separating the internal implementation of this symbol provider into two symbol providers wouldn't quite work without additional
 * disambiguation logic. That's because the "get symbol" functions for known declarations (i.e. those defined in [LLKotlinSymbolProvider])
 * would need to disambiguate whether to delegate to the regular symbol provider or the resolve extension symbol provider. If a declaration
 * from a resolve extension is passed to the regular symbol provider, that symbol provider might cache the symbol for it. This wouldn't lead
 * to duplicate symbols because FIR files (from which the symbols are taken) are unique in the session, but we would still cache a symbol in
 * the wrong symbol provider.
 */
internal class LLKotlinSourceSymbolProvider private constructor(
    session: LLFirSession,
    private val moduleComponents: LLFirModuleResolveComponents,
    extensionTool: LLFirResolveExtensionTool?,
    searchScope: GlobalSearchScope,
    canContainKotlinPackage: Boolean,
    declarationProviderFactory: (GlobalSearchScope) -> KotlinDeclarationProvider?,
) : LLKotlinSymbolProvider(session) {
    constructor(
        session: LLFirSession,
        moduleComponents: LLFirModuleResolveComponents,
        searchScope: GlobalSearchScope,
        canContainKotlinPackage: Boolean,
        declarationProviderFactory: (GlobalSearchScope) -> KotlinDeclarationProvider?,
    ) : this(session, moduleComponents, session.llResolveExtensionTool, searchScope, canContainKotlinPackage, declarationProviderFactory)

    override val declarationProvider = KotlinCompositeDeclarationProvider.create(
        listOfNotNull(
            declarationProviderFactory(searchScope),
            extensionTool?.declarationProvider,
        )
    )

    override val packageProvider = KotlinCompositePackageProvider.create(
        listOfNotNull(
            session.project.createPackageProvider(searchScope),
            extensionTool?.packageProvider,
        )
    )

    override val allowKotlinPackage: Boolean =
        canContainKotlinPackage || session.languageVersionSettings.getFlag(AnalysisFlags.allowKotlinPackage)

    override val symbolNamesProvider: FirSymbolNamesProvider = FirCompositeCachedSymbolNamesProvider.create(
        session,
        listOfNotNull(
            LLFirKotlinSymbolNamesProvider(declarationProvider, allowKotlinPackage),
            extensionTool?.symbolNamesProvider,
        )
    )

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        if (!symbolNamesProvider.mayHaveTopLevelClassifier(classId)) return null
        return getClassLikeSymbolByClassIdAndDeclaration(classId, classLikeDeclaration = null)
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId, classLikeDeclaration: KtClassLikeDeclaration): FirClassLikeSymbol<*>? {
        return getClassLikeSymbolByClassIdAndDeclaration(classId, classLikeDeclaration)
    }

    private val classifierCache: FirCache<ClassId, FirClassLikeSymbol<*>?, KtClassLikeDeclaration?> =
        session.firCachesFactory.createCache { classId, context ->
            computeClassLikeSymbolByClassId(classId, context)
        }

    private fun getClassLikeSymbolByClassIdAndDeclaration(
        classId: ClassId,
        classLikeDeclaration: KtClassLikeDeclaration?,
    ): FirClassLikeSymbol<*>? {
        if (classId.isLocal) return null
        if (!allowKotlinPackage && classId.isKotlinPackage()) return null
        return classifierCache.getNotNullValueForNotNullContext(classId, classLikeDeclaration)
    }

    private fun computeClassLikeSymbolByClassId(classId: ClassId, context: KtClassLikeDeclaration?): FirClassLikeSymbol<*>? {
        require(context == null || context.isPhysical)
        val ktClass = context ?: declarationProvider.getClassLikeDeclarationByClassId(classId) ?: return null

        if (ktClass.getClassId() == null) return null
        val firFile = moduleComponents.firFileBuilder.buildRawFirFileWithCaching(ktClass.containingKtFile)
        return FirElementFinder.findClassifierWithClassId(firFile, classId)?.symbol
            ?: errorWithAttachment("Classifier was found in KtFile but was not found in FirFile") {
                withEntry("classifierClassId", classId) { it.asString() }
                withVirtualFileEntry("virtualFile", ktClass.containingKtFile.virtualFile)
            }
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return emptyList()
        return getTopLevelCallableSymbols(CallableId(packageFqName, name), callableFiles = null)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return
        destination += getTopLevelCallableSymbols(CallableId(packageFqName, name), callableFiles = null)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<FirCallableSymbol<*>>,
        callableId: CallableId,
        callables: Collection<KtCallableDeclaration>
    ) {
        destination += getTopLevelCallableSymbols(callableId, callables.mapTo(mutableSetOf()) { it.containingKtFile })
    }

    private fun getTopLevelCallableSymbols(callableId: CallableId, callableFiles: Collection<KtFile>?): List<FirCallableSymbol<*>> {
        if (!allowKotlinPackage && callableId.packageName.isKotlinPackage()) return emptyList()

        val functions = getTopLevelFunctionSymbols(callableId, callableFiles)
        val properties = getTopLevelPropertySymbols(callableId, callableFiles)

        return functions.smartPlus(properties)
    }

    override fun getTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<FirNamedFunctionSymbol> {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return emptyList()
        return getTopLevelFunctionSymbols(CallableId(packageFqName, name), callableFiles = null)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return
        destination += getTopLevelFunctionSymbols(CallableId(packageFqName, name), callableFiles = null)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<FirNamedFunctionSymbol>,
        callableId: CallableId,
        functions: Collection<KtNamedFunction>
    ) {
        destination += getTopLevelFunctionSymbols(callableId, functions.mapTo(mutableSetOf()) { it.containingKtFile })
    }

    private val functionCache: FirCache<CallableId, List<FirNamedFunctionSymbol>, Collection<KtFile>?> =
        session.firCachesFactory.createCache { callableId, context ->
            computeCallableSymbolsByCallableId<FirNamedFunctionSymbol>(callableId, context)
        }

    private fun getTopLevelFunctionSymbols(callableId: CallableId, callableFiles: Collection<KtFile>?): List<FirNamedFunctionSymbol> {
        return functionCache.getValue(callableId, callableFiles)
    }

    override fun getTopLevelPropertySymbols(packageFqName: FqName, name: Name): List<FirPropertySymbol> {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return emptyList()
        return getTopLevelPropertySymbols(CallableId(packageFqName, name), callableFiles = null)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return
        destination += getTopLevelPropertySymbols(CallableId(packageFqName, name), callableFiles = null)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<FirPropertySymbol>,
        callableId: CallableId,
        properties: Collection<KtProperty>
    ) {
        destination += getTopLevelPropertySymbols(callableId, properties.mapTo(mutableSetOf()) { it.containingKtFile })
    }

    private val propertyCache: FirCache<CallableId, List<FirPropertySymbol>, Collection<KtFile>?> =
        session.firCachesFactory.createCache { callableId, context ->
            computeCallableSymbolsByCallableId<FirPropertySymbol>(callableId, context)
        }

    private fun getTopLevelPropertySymbols(callableId: CallableId, callableFiles: Collection<KtFile>?): List<FirPropertySymbol> {
        return propertyCache.getValue(callableId, callableFiles)
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
                val firFile = moduleComponents.firFileBuilder.buildRawFirFileWithCaching(ktFile)
                firFile.collectCallableSymbolsOfTypeTo<TYPE>(this, callableId.callableName)
            }
        }
    }

    private inline fun <reified TYPE : FirCallableSymbol<*>> FirFile.collectCallableSymbolsOfTypeTo(list: MutableList<TYPE>, name: Name) {
        declarations.mapNotNullTo(list) { declaration ->
            if (declaration is FirCallableDeclaration && declaration.symbol.callableId.callableName == name) {
                declaration.symbol as? TYPE
            } else null
        }
    }

    override fun hasPackage(fqName: FqName): Boolean {
        if (!allowKotlinPackage && fqName.isKotlinPackage()) return false
        return packageProvider.doesKotlinOnlyPackageExist(fqName)
    }
}

private fun ClassId.isKotlinPackage(): Boolean = startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)
private fun FqName.isKotlinPackage(): Boolean = startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)
