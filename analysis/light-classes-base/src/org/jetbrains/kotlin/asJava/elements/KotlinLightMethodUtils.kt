/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtPsiUtil

fun KtLightMethod.isTraitFakeOverride(): Boolean {
    val methodOrigin = this.kotlinOrigin
    if (!(methodOrigin is KtNamedFunction || methodOrigin is KtPropertyAccessor || methodOrigin is KtProperty)) {
        return false
    }

    val parentOfMethodOrigin = PsiTreeUtil.getParentOfType(methodOrigin, KtClassOrObject::class.java)
    val thisClassDeclaration = this.containingClass.kotlinOrigin

    // Method was generated from declaration in some other trait
    return (parentOfMethodOrigin != null && thisClassDeclaration !== parentOfMethodOrigin && KtPsiUtil.isTrait(parentOfMethodOrigin))
}

fun KtLightMethod.isAccessor(getter: Boolean): Boolean {
    val origin = kotlinOrigin as? KtCallableDeclaration ?: return false
    if (origin !is KtProperty && origin !is KtParameter) return false
    val expectedParametersCount = (if (getter) 0 else 1) + (if (origin.receiverTypeReference != null) 1 else 0)
    return parameterList.parametersCount == expectedParametersCount
}

val KtLightMethod.isGetter: Boolean
    get() = isAccessor(true)

val KtLightMethod.isSetter: Boolean
    get() = isAccessor(false)
