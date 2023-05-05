/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.components.psiDeclarationProvider

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter

public object TestPsiElementRenderer {
    public fun render(psiElement: PsiElement): String = when (psiElement) {
        is KtNamedFunction -> buildString {
            append("KtNamedFunction:")
            append(psiElement.name)
            append("(")
            psiElement.valueParameters.joinTo(this) { render(it) }
            append(")")
        }
        is KtParameter -> buildString {
            if (psiElement.isVarArg) {
                append("vararg ")
            }
            append(psiElement.name)
        }
        is KtElement -> psiElement.text
        is PsiMethod -> buildString {
            append("PsiMethod:")
            append(psiElement.name)
            append("(")
            psiElement.parameterList.parameters.joinTo(this) { render(it) }
            append("): ")
            append(psiElement.returnType)
        }
        is PsiParameter -> buildString {
            if (psiElement.isVarArgs) {
                append("vararg ")
            }
            append(psiElement.name)
            append(": ")
            append(psiElement.type)
        }
        else -> psiElement.toString()
    }
}