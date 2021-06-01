/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.util

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredFunctionSymbols
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredPropertySymbols
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.KtDeclarationAndFirDeclarationEqualityChecker
import org.jetbrains.kotlin.idea.fir.low.level.api.api.InvalidFirElementTypeException
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

/**
 * 'Non-local' stands for not local classes/functions/etc.
 */
internal fun KtDeclaration.findSourceNonLocalFirDeclaration(
    firFileBuilder: FirFileBuilder,
    firSymbolProvider: FirSymbolProvider,
    moduleFileCache: ModuleFileCache,
    containerFirFile: FirFile? = null
): FirDeclaration {
    //TODO test what way faster
    findSourceNonLocalFirDeclarationByProvider(firFileBuilder, firSymbolProvider, moduleFileCache, containerFirFile)?.let { return it }
    findSourceOfNonLocalFirDeclarationByTraversingWholeTree(firFileBuilder, moduleFileCache, containerFirFile)?.let { return it }
    error("No fir element was found for\n${getElementTextInContext()}")
}

internal fun KtDeclaration.findFirDeclarationForAnyFirSourceDeclaration(
    firFileBuilder: FirFileBuilder,
    firSymbolProvider: FirSymbolProvider,
    moduleFileCache: ModuleFileCache
): FirDeclaration {
    val nonLocalDeclaration = getNonLocalContainingOrThisDeclaration()
        ?.findSourceNonLocalFirDeclaration(firFileBuilder, firSymbolProvider, moduleFileCache)
        ?: firFileBuilder.buildRawFirFileWithCaching(containingKtFile, moduleFileCache, lazyBodiesMode = true)
    val originalDeclaration = originalDeclaration
    val fir = FirElementFinder.findElementIn<FirDeclaration>(nonLocalDeclaration) { firDeclaration ->
        firDeclaration.psi == this || firDeclaration.psi == originalDeclaration
    }
    return fir
        ?: error("FirDeclaration was not found for\n${getElementTextInContext()}")
}

internal inline fun <reified F : FirDeclaration> KtDeclaration.findFirDeclarationForAnyFirSourceDeclarationOfType(
    firFileBuilder: FirFileBuilder,
    firSymbolProvider: FirSymbolProvider,
    moduleFileCache: ModuleFileCache
): FirDeclaration {
    val fir = findFirDeclarationForAnyFirSourceDeclaration(firFileBuilder, firSymbolProvider, moduleFileCache)
    if (fir !is F) throw InvalidFirElementTypeException(this, F::class, fir::class)
    return fir
}

internal fun representSameFunction(psiFunction: KtNamedFunction, it: FirFunction<*>): Boolean =
    KtDeclarationAndFirDeclarationEqualityChecker.representsTheSameDeclaration(psiFunction, it)

internal fun representSameConstructor(psiConstructor: KtConstructor<*>, firConstructor: FirConstructor): Boolean {
    if ((firConstructor.isPrimary) != (psiConstructor is KtPrimaryConstructor)) {
        return false
    }

    return KtDeclarationAndFirDeclarationEqualityChecker.representsTheSameDeclaration(psiConstructor, firConstructor)
}

internal fun KtCallableDeclaration.findFunctionCandidates(
    symbolProvider: FirSymbolProvider,
    isTopLevel: Boolean
): List<FirFunctionSymbol<*>> =
    findCallableCandidates(symbolProvider, isTopLevel).filterIsInstance<FirFunctionSymbol<*>>()

internal fun KtCallableDeclaration.findPropertyCandidates(
    symbolProvider: FirSymbolProvider,
    isTopLevel: Boolean
): List<FirPropertySymbol> =
    findCallableCandidates(symbolProvider, isTopLevel).filterIsInstance<FirPropertySymbol>()

private fun KtCallableDeclaration.findCallableCandidates(
    symbolProvider: FirSymbolProvider,
    isTopLevel: Boolean
): List<FirCallableSymbol<*>> {
    if (isTopLevel) {
        return symbolProvider.getTopLevelCallableSymbols(containingKtFile.packageFqName, nameAsSafeName)
    }

    val containerClassId = containingClassOrObject?.getClassId()
        ?: error("No containing non-local declaration found for ${getElementTextInContext()}")

    return symbolProvider.getClassDeclaredFunctionSymbols(containerClassId, nameAsSafeName) +
            symbolProvider.getClassDeclaredPropertySymbols(containerClassId, nameAsSafeName)
}

private fun KtDeclaration.findSourceOfNonLocalFirDeclarationByTraversingWholeTree(
    firFileBuilder: FirFileBuilder,
    moduleFileCache: ModuleFileCache,
    containerFirFile: FirFile?,
): FirDeclaration? {
    val firFile = containerFirFile ?: firFileBuilder.buildRawFirFileWithCaching(containingKtFile, moduleFileCache, lazyBodiesMode = true)
    val originalDeclaration = originalDeclaration
    return FirElementFinder.findElementIn(firFile, goInside = { it is FirRegularClass }) { firDeclaration ->
        firDeclaration.psi == this || firDeclaration.psi == originalDeclaration
    }
}

private fun KtDeclaration.findSourceNonLocalFirDeclarationByProvider(
    firFileBuilder: FirFileBuilder,
    firSymbolProvider: FirSymbolProvider,
    moduleFileCache: ModuleFileCache,
    containerFirFile: FirFile?
): FirDeclaration? {
    val candidate = when {
        this is KtClassOrObject -> findFir(firSymbolProvider)
        this is KtNamedDeclaration && (this is KtProperty || this is KtNamedFunction) -> {
            val containerClass = containingClassOrObject
            val declarations = if (containerClass != null) {
                val containerClassFir = containerClass.findFir(firSymbolProvider) as? FirRegularClass
                containerClassFir?.declarations
            } else {
                val ktFile = containingKtFile
                val firFile = containerFirFile ?: firFileBuilder.buildRawFirFileWithCaching(ktFile, moduleFileCache, lazyBodiesMode = true)
                firFile.declarations
            }
            val original = originalDeclaration

            /*
            It is possible that we will not be able to find needed declaration here when the code is invalid,
            e.g, we have two conflicting declarations with the same name and we are searching in the wrong one
             */
            declarations?.firstOrNull { it.psi == this || it.psi == original }
        }
        this is KtConstructor<*> -> {
            val containingClass = containingClassOrObject
                ?: error("Container class should be not null for KtConstructor")
            val containerClassFir = containingClass.findFir(firSymbolProvider) as? FirRegularClass ?: return null
            containerClassFir.declarations.firstOrNull { it.psi === this }
        }
        this is KtTypeAlias -> findFir(firSymbolProvider)
        else -> error("Invalid container ${this::class}\n${getElementTextInContext()}")
    }
    return candidate?.takeIf { it.realPsi == this }
}

val ORIGINAL_DECLARATION_KEY = com.intellij.openapi.util.Key<KtDeclaration>("ORIGINAL_DECLARATION_KEY")
var KtDeclaration.originalDeclaration by UserDataProperty(ORIGINAL_DECLARATION_KEY)

private val ORIGINAL_KT_FILE_KEY = com.intellij.openapi.util.Key<KtFile>("ORIGINAL_KT_FILE_KEY")
var KtFile.originalKtFile by UserDataProperty(ORIGINAL_KT_FILE_KEY)


private fun KtClassLikeDeclaration.findFir(firSymbolProvider: FirSymbolProvider): FirClassLikeDeclaration<*>? {
    val classId = getClassId() ?: return null
    return executeWithoutPCE {
        firSymbolProvider.getClassLikeSymbolByFqName(classId)?.fir as? FirRegularClass
    }
}


val FirDeclaration.isGeneratedDeclaration
    get() = realPsi == null
