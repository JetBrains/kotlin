/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
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
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.providers.FirCompositeCachedSymbolNamesProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
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

    private val classifierByClassId: FirCache<KeyWithPsiContext<ClassId>, FirClassLikeDeclaration?, KtClassLikeDeclaration?> =
        firSession.firCachesFactory.createCache { (classId, _), context ->
            require(context == null || context.isPhysical)
            val ktClass = context ?: declarationProvider.getClassLikeDeclarationByClassId(classId) ?: return@createCache null

            if (ktClass.getClassId() == null) return@createCache null
            val firFile = firFileBuilder.buildRawFirFileWithCaching(ktClass.containingKtFile)
            FirElementFinder.findClassifierWithClassId(firFile, classId)
                ?: errorWithAttachment("Classifier was found in KtFile but was not found in FirFile") {
                    withEntry("classifierClassId", classId) { it.asString() }
                    withVirtualFileEntry("virtualFile", ktClass.containingKtFile.virtualFile)
                }
        }

    private val callablesByCallableId: FirCache<KeyWithPsiContext<CallableId>, List<FirCallableSymbol<*>>, Collection<KtFile>?> =
        firSession.firCachesFactory.createCache { (callableId, _), context ->
            require(context == null || context.all { it.isPhysical })
            val files = context ?: declarationProvider.getTopLevelCallableFiles(callableId).ifEmpty { return@createCache emptyList() }
            buildList {
                files.forEach { ktFile ->
                    val firFile = firFileBuilder.buildRawFirFileWithCaching(ktFile)
                    firFile.collectCallableDeclarationsTo(this, callableId.callableName)
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

    fun getFirClassifierByFqNameAndDeclaration(
        classId: ClassId,
        classLikeDeclaration: KtClassLikeDeclaration?,
    ): FirClassLikeDeclaration? {
        if (classId.isLocal) return null
        if (!allowKotlinPackage && classId.isKotlinPackage()) return null
        return classifierByClassId.getNotNullValueForNotNullContext(
            KeyWithPsiContext.create(classId, classLikeDeclaration?.let(::setOf)),
            classLikeDeclaration
        )
    }

    fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        if (!allowKotlinPackage && packageFqName.isKotlinPackage()) return emptyList()
        val callableId = CallableId(packageFqName, name)
        return callablesByCallableId.getValue(KeyWithPsiContext.create(callableId))
    }

    /**
     * [callableFiles] are the [KtFile]s which contain callables of the given package and name. If already known, they can be provided to
     * avoid index accesses.
     */
    fun getTopLevelCallableSymbols(callableId: CallableId, callableFiles: Collection<KtFile>?): List<FirCallableSymbol<*>> {
        if (!allowKotlinPackage && callableId.packageName.isKotlinPackage()) return emptyList()
        return callablesByCallableId.getValue(KeyWithPsiContext.create(callableId, callableFiles), callableFiles)
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

/**
 * This class allows creating unique keys for an arbitrary [KEY] and a set of [PsiElement]s.
 *
 * This allows to use [FirCache] in a situation when the stored result should be associated not only with
 * some immutable [key] value, but also with a set of [PsiElement]s supplied as a context.
 *
 * To not hold the strong references onto the [PsiElement]s, [SmartPsiElementPointer] is used.
 *
 * [contextPsiElementPointers] are not supposed to be used to create the cache's values; they are only
 * needed to make the [KeyWithPsiContext] instances unique inside the cache.
 *
 * Do not use the constructor directly - use [create] method instead.
 */
private data class KeyWithPsiContext<KEY>(
    val key: KEY,
    val contextPsiElementPointers: Set<SmartPsiElementPointer<PsiElement>>?,
) {
    companion object {

        /**
         * N.B. When you call `create(key, null)` and `create(key, emptyList())`, it results in a pair of objects
         * which are **not equal to each other**!
         *
         * Pay attention to that when you implement the value computation for your [FirCache].
         */
        fun <KEY> create(id: KEY, contextPsiElements: Collection<PsiElement>? = null): KeyWithPsiContext<KEY> {
            val pointers = contextPsiElements
                ?.map {
                    @Suppress("DEPRECATION")
                    it.createSmartPointer()
                }
                ?.toSet()

            return KeyWithPsiContext(id, pointers)
        }
    }
}
