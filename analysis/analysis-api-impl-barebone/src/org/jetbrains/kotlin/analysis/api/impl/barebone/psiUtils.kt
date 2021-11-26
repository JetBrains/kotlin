/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.barebone

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

inline fun <reified T : PsiElement> PsiElement.parentOfType(withSelf: Boolean = false): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, !withSelf)
}

fun <T : PsiElement> PsiElement.parentsOfType(clazz: Class<out T>, withSelf: Boolean = true): Sequence<T> {
    return (if (withSelf) parentsWithSelf else parents).filterIsInstance(clazz)
}

inline fun <reified T : PsiElement> PsiElement.parentsOfType(withSelf: Boolean = true): Sequence<T> =
    parentsOfType(T::class.java, withSelf)
