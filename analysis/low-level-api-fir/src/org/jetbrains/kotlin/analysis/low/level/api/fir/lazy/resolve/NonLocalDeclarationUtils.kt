/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

/**
 * Note: The `KtCodeFragment` itself is technically lazy-resolvable, but the function doesn't support it yet.
 */
internal fun elementCanBeLazilyResolved(element: KtElement?): Boolean = when (element) {
    null -> false
    is KtFunctionLiteral -> false
    is KtTypeParameter -> elementCanBeLazilyResolved(element.parentOfType<KtNamedDeclaration>(withSelf = false))
    is KtScript -> elementCanBeLazilyResolved(element.parent as? KtFile)
    is KtFile -> element !is KtCodeFragment
    is KtDestructuringDeclarationEntry -> elementCanBeLazilyResolved(element.parent as? KtDestructuringDeclaration)
    is KtParameter -> elementCanBeLazilyResolved(element.ownerDeclaration)
    is KtCallableDeclaration, is KtEnumEntry, is KtDestructuringDeclaration, is KtAnonymousInitializer -> {
        val parentToCheck = when (val parent = element.parent) {
            is KtClassOrObject, is KtFile -> parent
            is KtClassBody -> parent.parent as? KtClassOrObject
            is KtBlockExpression -> parent.parent as? KtScript
            else -> null
        }

        elementCanBeLazilyResolved(parentToCheck.takeUnless { it is KtEnumEntry })
    }

    is KtPropertyAccessor -> elementCanBeLazilyResolved(element.property)
    is KtClassOrObject -> element.isTopLevel() || element.getClassId() != null
    is KtTypeAlias -> element.isTopLevel() || element.getClassId() != null
    is KtModifierList -> element.isNonLocalDanglingModifierList()
    !is KtNamedDeclaration -> false
    else -> errorWithAttachment("Unexpected ${element::class}") {
        withPsiEntry("declaration", element)
    }
}

/**
 * Detects a common pattern of invalid code where a modifier list (e.g., annotation)
 * is dangling—unattached to a valid declaration—or left unclosed and followed by another declaration.
 *
 * ### Examples
 *
 * ```kotlin
 * class C1 {
 *     @Ann1 @Ann2
 * }
 *
 * class C2 {
 *     @Ann(
 *     fun foo() {}
 * }
 *
 * @Ann("argument"
 * fun foo() {}
 * ```
 * @see org.jetbrains.kotlin.fir.declarations.FirDanglingModifierList
 * @see org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder.Visitor.buildErrorNonLocalDeclarationForDanglingModifierList
 */
private fun KtModifierList.isNonLocalDanglingModifierList(): Boolean {
    val parentToCheck = when (val parent = parent) {
        is KtFile -> parent
        is KtClassBody -> (parent.parent as? KtClassOrObject).takeUnless { it is KtEnumEntry }
        else -> null
    }

    return elementCanBeLazilyResolved(parentToCheck)
}
