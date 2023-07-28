/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.KtFakeSourceElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal fun FirDeclaration.getKtDeclarationForFirElement(): KtDeclaration {
    require(this !is FirFile)

    val ktDeclaration = (psi as? KtDeclaration) ?: run {
        (source as? KtFakeSourceElement).psi?.parentOfType()
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
                    check(this.source?.kind == KtFakeSourceElementKind.DefaultAccessor)
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

internal fun declarationCanBeLazilyResolved(declaration: KtDeclaration): Boolean = when (declaration) {
    is KtDestructuringDeclarationEntry, is KtFunctionLiteral, is KtTypeParameter -> false
    is KtPrimaryConstructor -> (declaration.parent as? KtClassOrObject)?.isLocal == false
    is KtParameter -> declaration.hasValOrVar() && declaration.containingClassOrObject?.isLocal == false
    is KtCallableDeclaration, is KtEnumEntry, is KtClassInitializer -> {
        when (val parent = declaration.parent) {
            is KtFile -> true
            is KtClassBody -> (parent.parent as? KtClassOrObject)?.isLocal == false
            is KtBlockExpression -> parent.parent is KtScript
            else -> false
        }
    }
    !is KtNamedDeclaration -> false
    is KtClassOrObject -> !declaration.isLocal
    is KtTypeAlias -> declaration.isTopLevel() || declaration.getClassId() != null
    else -> errorWithAttachment("Unexpected ${declaration::class}") {
        withPsiEntry("declaration", declaration)
    }
}
