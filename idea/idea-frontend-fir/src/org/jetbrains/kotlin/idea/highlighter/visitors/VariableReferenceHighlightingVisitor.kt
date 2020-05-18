/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter.visitors

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiVariable
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.KotlinIdeaAnalysisBundle
import org.jetbrains.kotlin.idea.frontend.api.FrontendAnalysisSession
import org.jetbrains.kotlin.idea.highlighter.NameHighlighter
import org.jetbrains.kotlin.idea.highlighter.textAttributesKeyForPropertyDeclaration
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors as Colors

internal class VariableReferenceHighlightingVisitor(
    analysisSession: FrontendAnalysisSession,
    holder: AnnotationHolder
) : FirAfterResolveHighlightingVisitor(analysisSession, holder) {
    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        if (!NameHighlighter.namesHighlightingEnabled) return
        if (expression.isAssignmentReference()) return
        if (expression.parent is KtInstanceExpressionWithLabel) return

        if (expression.isAutoCreatedItParameter()) {
            createInfoAnnotation(
                expression,
                Colors.FUNCTION_LITERAL_DEFAULT_PARAMETER,
                KotlinIdeaAnalysisBundle.message("automatically.declared.based.on.the.expected.type")
            )
            return
        }

        val target = expression.mainReference.resolve()

        when {
            target != null && expression.isBackingField(target) -> Colors.BACKING_FIELD_VARIABLE
            target is PsiMethod -> Colors.SYNTHETIC_EXTENSION_PROPERTY
            target != null -> textAttributesKeyForPropertyDeclaration(target)
            else -> null
        }?.let { attribute ->
            highlightName(expression, attribute)
            if (target?.isMutableVariable() == true) {
                highlightName(expression, Colors.MUTABLE_VARIABLE)
            }
        }
    }


    private fun KtSimpleNameExpression.isAutoCreatedItParameter(): Boolean {
        return getReferencedName() == "it" // todo
    }
}

private fun PsiElement.isMutableVariable() = when {
    this is KtValVarKeywordOwner && PsiUtilCore.getElementType(valOrVarKeyword) == KtTokens.VAR_KEYWORD -> true
    this is PsiVariable && !hasModifierProperty(PsiModifier.FINAL) -> true
    else -> false
}

private fun KtSimpleNameExpression.isAssignmentReference(): Boolean {
    if (this !is KtOperationReferenceExpression) return false
    return operationSignTokenType == KtTokens.EQ
}

private fun KtSimpleNameExpression.isBackingField(target: PsiElement): Boolean {
    if (getReferencedName() != "field") return false
    if (target !is KtProperty) return false
    val accessor = parentOfType<KtPropertyAccessor>() ?: return false
    return accessor.parent == target
}
