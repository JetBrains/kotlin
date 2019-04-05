/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.idea.core.util.CodeInsightUtils
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile

fun getContextElement(elementAt: PsiElement?): PsiElement? {
    if (elementAt == null) return null

    if (elementAt is PsiCodeBlock) {
        return getContextElement(elementAt.context?.context)
    }

    if (elementAt is KtLightClass) {
        return getContextElement(elementAt.kotlinOrigin)
    }

    val containingFile = elementAt.containingFile
    if (containingFile is PsiJavaFile) return elementAt
    if (containingFile !is KtFile) return null

    // elementAt can be PsiWhiteSpace when codeFragment is created from line start offset (in case of first opening EE window)
    val lineStartOffset = if (elementAt is PsiWhiteSpace || elementAt is PsiComment) {
        PsiTreeUtil.skipSiblingsForward(elementAt, PsiWhiteSpace::class.java, PsiComment::class.java)?.textOffset
            ?: elementAt.textOffset
    } else {
        elementAt.textOffset
    }

    fun KtElement.takeIfAcceptedAsCodeFragmentContext() = takeIf { KotlinEditorTextProvider.isAcceptedAsCodeFragmentContext(it) }

    PsiTreeUtil.findElementOfClassAtOffset(containingFile, lineStartOffset, KtExpression::class.java, false)
        ?.takeIfAcceptedAsCodeFragmentContext()
        ?.let { return CodeInsightUtils.getTopmostElementAtOffset(it, lineStartOffset, KtExpression::class.java) }

    KotlinEditorTextProvider.findExpressionInner(elementAt, true)
        ?.takeIfAcceptedAsCodeFragmentContext()
        ?.let { return it }

    return containingFile
}