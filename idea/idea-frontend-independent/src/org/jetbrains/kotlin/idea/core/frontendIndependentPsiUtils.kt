/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtPsiUtil

inline fun <reified T : PsiElement> PsiElement.replaced(newElement: T): T {
    val result = replace(newElement)
    return result as? T ?: (result as KtParenthesizedExpression).expression as T
}

//todo make inline
@Suppress("UNCHECKED_CAST")
fun <T : PsiElement> T.copied(): T = copy() as T

fun String.unquote(): String = KtPsiUtil.unquoteIdentifier(this)



