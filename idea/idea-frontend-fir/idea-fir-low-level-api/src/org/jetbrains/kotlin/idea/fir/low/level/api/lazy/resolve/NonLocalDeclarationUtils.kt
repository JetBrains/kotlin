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
import org.jetbrains.kotlin.idea.fir.low.level.api.api.*
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.util.findSourceNonLocalFirDeclaration
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

internal fun FirDeclaration.getKtDeclarationForFirElement(): KtDeclaration {
    require(this !is FirFile)

    val ktDeclaration = (psi as? KtDeclaration) ?: run {
        (source as? FirFakeSourceElement<*>).psi?.parentOfType()
    }
    check(ktDeclaration is KtDeclaration) {
        "FirDeclaration should have a PSI of type KtDeclaration"
    }

    val declaration = when (this) {
        is FirPropertyAccessor, is FirTypeParameter, is FirValueParameter -> {
            when (ktDeclaration) {
                is KtPropertyAccessor -> ktDeclaration.property
                is KtProperty -> ktDeclaration
                is KtParameter, is KtTypeParameter -> {
                    val containingDeclaration = ktDeclaration.getParentOfType<KtDeclaration>(true)
                    if (containingDeclaration !is KtPropertyAccessor) containingDeclaration else containingDeclaration.property
                }
                is KtCallExpression -> {
                    check(this.source?.kind == FirFakeSourceElementKind.DefaultAccessor)
                    ((ktDeclaration as? KtCallExpression)?.parent as? KtPropertyDelegate)?.parent as? KtProperty
                }
                else -> ktDeclaration
            }
        }
        else -> ktDeclaration
    }
    check(declaration is KtDeclaration) {
        "FirDeclaration should have a PSI of type KtDeclaration"
    }
    return declaration
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