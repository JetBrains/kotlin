/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run

import com.intellij.execution.Location
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.idea.caches.lightClasses.KtFakeLightClass
import org.jetbrains.kotlin.idea.caches.lightClasses.KtFakeLightMethod
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

internal fun getTestMethodForKotlinTest(location: Location<*>): PsiMethod? {
    val leaf = location.psiElement
    val function = leaf?.getParentOfType<KtNamedFunction>(false) ?: return null
    return KtFakeLightMethod.get(function)
}

internal fun getTestClassForKotlinTest(location: Location<*>): PsiClass? {
    val leaf = location.psiElement
    val owner = leaf?.getParentOfType<KtDeclaration>(true) as? KtClassOrObject ?: return null
    return KtFakeLightClass(owner)
}