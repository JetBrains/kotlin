/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run

import com.intellij.execution.Location
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

internal fun getTestMethodForKotlinTest(location: Location<*>): PsiMethod? {
    val leaf = location.psiElement
    val function = leaf?.getParentOfType<KtNamedFunction>(false) ?: return null
    val owner = function.getParentOfType<KtDeclaration>(true) as? KtClass ?: return null
    val delegate = owner.toLightClass() ?: return null
    return delegate.methods.firstOrNull { it.navigationElement == function } ?: return null
}

internal fun getTestClassForKotlinTest(location: Location<*>): PsiClass? {
    val leaf = location.psiElement
    val owner = leaf?.getParentOfType<KtDeclaration>(true) as? KtClass ?: return null
    return owner.toLightClass()
}