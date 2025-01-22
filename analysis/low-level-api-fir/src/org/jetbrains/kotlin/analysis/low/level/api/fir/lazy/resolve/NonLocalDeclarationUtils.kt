/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.isReplSnippet
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal fun declarationCanBeLazilyResolved(element: KtElement?, codeFragmentAware: Boolean): Boolean = when (element) {
    null -> false
    is KtFunctionLiteral -> false
    is KtTypeParameter -> declarationCanBeLazilyResolved(element.parentOfType<KtNamedDeclaration>(withSelf = false), codeFragmentAware)
    is KtScript -> declarationCanBeLazilyResolved(element.parent as? KtFile, codeFragmentAware)
    is KtFile -> codeFragmentAware || element !is KtCodeFragment || element.isReplSnippet()
    is KtDestructuringDeclarationEntry -> declarationCanBeLazilyResolved(element.parent as? KtDestructuringDeclaration, codeFragmentAware)
    is KtParameter -> declarationCanBeLazilyResolved(element.ownerFunction, codeFragmentAware)
    is KtCallableDeclaration, is KtEnumEntry, is KtDestructuringDeclaration, is KtAnonymousInitializer -> {
        val parentToCheck = when (val parent = element.parent) {
            is KtClassOrObject, is KtFile -> parent
            is KtClassBody -> parent.parent as? KtClassOrObject
            is KtBlockExpression -> parent.parent as? KtScript
            else -> null
        }

        declarationCanBeLazilyResolved(parentToCheck.takeUnless { it is KtEnumEntry }, codeFragmentAware)
    }

    is KtPropertyAccessor -> declarationCanBeLazilyResolved(element.property, codeFragmentAware)
    is KtClassOrObject -> element.isTopLevel() || element.getClassId() != null
    is KtTypeAlias -> element.isTopLevel() || element.getClassId() != null
    !is KtNamedDeclaration -> false
    else -> errorWithAttachment("Unexpected ${element::class}") {
        withPsiEntry("declaration", element)
    }
}
