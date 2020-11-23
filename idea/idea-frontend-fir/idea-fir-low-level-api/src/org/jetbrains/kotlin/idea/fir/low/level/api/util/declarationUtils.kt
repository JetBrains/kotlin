/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.util

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.api.InvalidFirElementTypeException
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.util.classIdIfNonLocal
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

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
                val containerClassFir = containerClass.findFir(firSymbolProvider)
                containerClassFir?.declarations
            } else {
                val ktFile = containingKtFile
                val firFile = containerFirFile ?: firFileBuilder.buildRawFirFileWithCaching(ktFile, moduleFileCache, lazyBodiesMode = true)
                firFile.declarations
            }
            val original = originalDeclaration
            declarations?.first { it.psi === this || it.psi == original }
        }
        this is KtConstructor<*> -> {
            val containingClass = containingClassOrObject
                ?: error("Container class should be not null for KtConstructor")
            val containerClassFir = containingClass.findFir(firSymbolProvider) ?: return null
            containerClassFir.declarations.first { it.psi === this }
        }
        this is KtTypeAlias -> findFir(firSymbolProvider)
        else -> error("Invalid container $this::class")
    }
    return candidate?.takeIf { it.realPsi == this }
}

val ORIGINAL_DECLARATION_KEY = com.intellij.openapi.util.Key<KtDeclaration>("ORIGINAL_DECLARATION_KEY")
var KtDeclaration.originalDeclaration by UserDataProperty(ORIGINAL_DECLARATION_KEY)

private val ORIGINAL_KT_FILE_KEY = com.intellij.openapi.util.Key<KtFile>("ORIGINAL_KT_FILE_KEY")
var KtFile.originalKtFile by UserDataProperty(ORIGINAL_KT_FILE_KEY)


private fun KtClassOrObject.findFir(firSymbolProvider: FirSymbolProvider): FirRegularClass? {
    val classId = classIdIfNonLocal() ?: return null
    return executeWithoutPCE {
        firSymbolProvider.getClassLikeSymbolByFqName(classId)?.fir as? FirRegularClass
            ?: error("Could not find class $classId")
    }
}

private fun KtTypeAlias.findFir(firSymbolProvider: FirSymbolProvider): FirTypeAlias {
    val typeAlias = ClassId(containingKtFile.packageFqName, nameAsSafeName)
    return executeWithoutPCE {
        firSymbolProvider.getClassLikeSymbolByFqName(typeAlias)?.fir as? FirTypeAlias
            ?: error("Could not find type alias $typeAlias")
    }
}

