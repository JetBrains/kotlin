/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve

import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.fir.FirFakeSourceElement
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.util.findSourceNonLocalFirDeclaration
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

internal fun FirDeclaration.getNonLocalDeclarationToResolve(
    provider: FirProvider,
    moduleFileCache: ModuleFileCache,
    firFileBuilder: FirFileBuilder
): FirDeclaration {
    if (this is FirFile) return this

    if (this is FirPropertyAccessor || this is FirTypeParameter || this is FirValueParameter) {
        val ktContainingResolvableDeclaration = when (val psi = this.psi) {
            is KtPropertyAccessor -> psi.property
            is KtProperty -> psi
            is KtParameter, is KtTypeParameter -> psi.getNonLocalContainingOrThisDeclaration()
                ?: error("Cannot find containing declaration for KtParameter")
            is KtCallExpression -> {
                check(this.source?.kind == FirFakeSourceElementKind.DefaultAccessor)
                val delegationCall = psi.parent as KtPropertyDelegate
                delegationCall.parent as KtProperty
            }
            null -> error("Cannot find containing declaration for KtParameter")
            else -> error("Invalid source of property accessor ${psi::class}")
        }

        val targetElement =
            if (declarationCanBeLazilyResolved(ktContainingResolvableDeclaration)) ktContainingResolvableDeclaration
            else ktContainingResolvableDeclaration.getNonLocalContainingOrThisDeclaration()
        check(targetElement != null) { "Container for local declaration cannot be null" }

        return targetElement.findSourceNonLocalFirDeclaration(
            firFileBuilder = firFileBuilder,
            firSymbolProvider = moduleData.session.symbolProvider,
            moduleFileCache = moduleFileCache
        )
    }

    val ktDeclaration = (psi as? KtDeclaration) ?: run {
        (source as? FirFakeSourceElement<*>).psi?.parentOfType()
    }
    check(ktDeclaration is KtDeclaration) {
        "FirDeclaration should have a PSI of type KtDeclaration"
    }

    if (source !is FirFakeSourceElement<*> && declarationCanBeLazilyResolved(ktDeclaration)) return this
    val nonLocalPsi = ktDeclaration.getNonLocalContainingOrThisDeclaration()
        ?: error("Container for local declaration cannot be null")
    return nonLocalPsi.findSourceNonLocalFirDeclaration(firFileBuilder, provider.symbolProvider, moduleFileCache)
}

internal fun declarationCanBeLazilyResolved(declaration: KtDeclaration): Boolean {
    return when (declaration) {
        !is KtNamedDeclaration -> false
        is KtDestructuringDeclarationEntry, is KtFunctionLiteral, is KtTypeParameter -> false
        is KtPrimaryConstructor -> false
        is KtParameter -> declaration.hasValOrVar() && declaration.containingClassOrObject?.getClassId() != null
        is KtCallableDeclaration, is KtEnumEntry -> {
            when (val parent = declaration.parent) {
                is KtFile -> true
                is KtClassBody -> (parent.parent as? KtClassOrObject)?.getClassId() != null
                else -> false
            }
        }
        is KtClassLikeDeclaration -> declaration.getClassId() != null
        else -> error("Unexpected ${declaration::class.qualifiedName}")
    }
}