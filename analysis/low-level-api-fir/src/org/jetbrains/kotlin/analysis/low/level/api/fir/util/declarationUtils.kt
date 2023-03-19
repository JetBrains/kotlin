/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirFileBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirProvider
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

/**
 * 'Non-local' stands for not local classes/functions/etc.
 */
internal fun KtDeclaration.findSourceNonLocalFirDeclaration(
    firFileBuilder: LLFirFileBuilder,
    provider: FirProvider,
    containerFirFile: FirFile? = null
): FirDeclaration {
    //TODO test what way faster
    findSourceNonLocalFirDeclarationByProvider(firFileBuilder, provider, containerFirFile)?.let { return it }
    findSourceByTraversingWholeTree(firFileBuilder, containerFirFile)?.let { return it }
    errorWithFirSpecificEntries("No fir element was found for", psi = this)
}

internal fun KtDeclaration.findFirDeclarationForAnyFirSourceDeclaration(
    firFileBuilder: LLFirFileBuilder,
    provider: FirProvider,
): FirDeclaration {
    val nonLocalDeclaration = getNonLocalContainingOrThisDeclaration()
        ?.findSourceNonLocalFirDeclaration(firFileBuilder, provider)
        ?: firFileBuilder.buildRawFirFileWithCaching(containingKtFile)
    val originalDeclaration = originalDeclaration
    val fir = FirElementFinder.findElementIn<FirDeclaration>(nonLocalDeclaration) { firDeclaration ->
        firDeclaration.psi == this || firDeclaration.psi == originalDeclaration
    }
    return fir
        ?: errorWithFirSpecificEntries("FirDeclaration was not found", psi = this)
}

internal inline fun <reified F : FirDeclaration> KtDeclaration.findFirDeclarationForAnyFirSourceDeclarationOfType(
    firFileBuilder: LLFirFileBuilder,
    provider: FirProvider,
): FirDeclaration {
    val fir = findFirDeclarationForAnyFirSourceDeclaration(firFileBuilder, provider)
    if (fir !is F) throwUnexpectedFirElementError(fir, this, F::class)
    return fir
}

internal fun KtElement.findSourceByTraversingWholeTree(
    firFileBuilder: LLFirFileBuilder,
    containerFirFile: FirFile?,
): FirDeclaration? {
    val firFile = containerFirFile ?: firFileBuilder.buildRawFirFileWithCaching(containingKtFile)
    val originalDeclaration = (this as? KtDeclaration)?.originalDeclaration
    val isDeclaration = this is KtDeclaration
    return FirElementFinder.findElementIn(
        firFile,
        canGoInside = { it is FirRegularClass || it is FirScript },
        predicate = { firDeclaration ->
            firDeclaration.psi == this || isDeclaration && firDeclaration.psi == originalDeclaration
        }
    )
}

private fun KtDeclaration.findSourceNonLocalFirDeclarationByProvider(
    firFileBuilder: LLFirFileBuilder,
    provider: FirProvider,
    containerFirFile: FirFile?
): FirDeclaration? {
    val candidate = when {
        this is KtClassOrObject -> findFir(provider)
        this is KtNamedDeclaration && (this is KtProperty || this is KtNamedFunction) -> {
            val containerClass = containingClassOrObject
            val declarations = if (containerClass != null) {
                val containerClassFir = containerClass.findFir(provider) as? FirRegularClass
                containerClassFir?.declarations
            } else {
                val ktFile = containingKtFile
                val firFile = containerFirFile ?: firFileBuilder.buildRawFirFileWithCaching(ktFile)
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
                ?: errorWithFirSpecificEntries("Container class should be not null for KtConstructor", psi = this)
            val containerClassFir = containingClass.findFir(provider) as? FirRegularClass ?: return null
            containerClassFir.declarations.firstOrNull { it.psi === this }
        }
        this is KtTypeAlias -> findFir(provider)
        this is KtDestructuringDeclaration -> {
            val firFile = containerFirFile ?: firFileBuilder.buildRawFirFileWithCaching(containingKtFile)
            firFile.declarations.firstOrNull { it.psi == this }
        }
        this is KtScript -> containerFirFile?.declarations?.singleOrNull { it is FirScript }
        else -> errorWithFirSpecificEntries("Invalid container", psi = this)
    }
    return candidate?.takeIf { it.realPsi == this }
}

val ORIGINAL_DECLARATION_KEY = com.intellij.openapi.util.Key<KtDeclaration>("ORIGINAL_DECLARATION_KEY")
var KtDeclaration.originalDeclaration by UserDataProperty(ORIGINAL_DECLARATION_KEY)

private val ORIGINAL_KT_FILE_KEY = com.intellij.openapi.util.Key<KtFile>("ORIGINAL_KT_FILE_KEY")
var KtFile.originalKtFile by UserDataProperty(ORIGINAL_KT_FILE_KEY)


private fun KtClassLikeDeclaration.findFir(provider: FirProvider): FirClassLikeDeclaration? {
    val declaration = if (provider is LLFirProvider) {
        provider.getFirClassifierByDeclaration(this)
    } else {
        val classId = getClassId() ?: return null
        provider.getFirClassifierByFqName(classId)
    }

    return declaration as? FirRegularClass
}


val FirDeclaration.isGeneratedDeclaration
    get() = realPsi == null
