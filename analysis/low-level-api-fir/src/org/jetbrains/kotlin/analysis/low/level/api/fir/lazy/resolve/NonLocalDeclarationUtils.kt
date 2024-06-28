/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

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
