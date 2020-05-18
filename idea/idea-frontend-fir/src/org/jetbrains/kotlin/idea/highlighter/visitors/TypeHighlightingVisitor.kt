/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter.visitors

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.frontend.api.FrontendAnalysisSession
import org.jetbrains.kotlin.idea.highlighter.NameHighlighter
import org.jetbrains.kotlin.idea.highlighter.isAnnotationClass
import org.jetbrains.kotlin.idea.highlighter.textAttributesKeyForTypeDeclaration
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors as Colors

internal class TypeHighlightingVisitor(
    analysisSession: FrontendAnalysisSession,
    holder: AnnotationHolder
) : FirAfterResolveHighlightingVisitor(analysisSession, holder) {
    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        if (!NameHighlighter.namesHighlightingEnabled) return
        if (expression.isCalleeExpression()) return
        val parent = expression.parent

        if (parent is KtInstanceExpressionWithLabel) {
            // Do nothing: 'super' and 'this' are highlighted as a keyword
            return
        }
        val target = expression.mainReference.resolve() ?: return
        textAttributesKeyForTypeDeclaration(target)?.let { key ->
            if (expression.isConstructorCallReference() && key != Colors.ANNOTATION) {
                // Do not highlight constructor call as class reference
                return@let
            }
            highlightName(computeHighlightingRangeForUsage(expression, target), key)
        }
    }


    private fun computeHighlightingRangeForUsage(expression: KtSimpleNameExpression, target: PsiElement): TextRange {
        val expressionRange = expression.textRange

        if (!target.isAnnotationClass()) return expressionRange

        // include '@' symbol if the reference is the first segment of KtAnnotationEntry
        // if "Deprecated" is highlighted then '@' should be highlighted too in "@Deprecated"
        val annotationEntry = PsiTreeUtil.getParentOfType(
            expression, KtAnnotationEntry::class.java, /* strict = */false, KtValueArgumentList::class.java
        )
        val atSymbol = annotationEntry?.atSymbol ?: return expressionRange
        return TextRange(atSymbol.textRange.startOffset, expression.textRange.endOffset)
    }
}

private fun KtSimpleNameExpression.isCalleeExpression() =
    (parent as? KtCallExpression)?.calleeExpression == this

private fun KtSimpleNameExpression.isConstructorCallReference(): Boolean {
    val type = parent as? KtUserType ?: return false
    val typeReference = type.parent as? KtTypeReference ?: return false
    val constructorCallee = typeReference.parent as? KtConstructorCalleeExpression ?: return false
    return constructorCallee.constructorReferenceExpression == this
}