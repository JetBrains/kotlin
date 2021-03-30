/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.highlighter.visitors

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiVariable
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.KotlinIdeaAnalysisBundle
import org.jetbrains.kotlin.idea.fir.highlighter.textAttributesKeyForPropertyDeclaration
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtBackingFieldSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.highlighter.NameHighlighter
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors as Colors

internal class VariableReferenceHighlightingVisitor(
    analysisSession: KtAnalysisSession,
    holder: AnnotationHolder
) : FirAfterResolveHighlightingVisitor(analysisSession, holder) {
    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        if (!NameHighlighter.namesHighlightingEnabled) return
        if (expression.isAssignmentReference()) return
        if (expression.isByNameArgumentReference()) return
        if (expression.parent is KtInstanceExpressionWithLabel) return

        if (expression.isAutoCreatedItParameter()) {
            createInfoAnnotation(
                expression,
                KotlinIdeaAnalysisBundle.message("automatically.declared.based.on.the.expected.type"),
                Colors.FUNCTION_LITERAL_DEFAULT_PARAMETER
            )
            return
        }

        with(analysisSession) {
            val targetSymbol = expression.mainReference.resolveToSymbol()
            val target = expression.mainReference.resolve()
            when {
                targetSymbol is KtBackingFieldSymbol -> Colors.BACKING_FIELD_VARIABLE
                target is PsiMethod -> Colors.SYNTHETIC_EXTENSION_PROPERTY
                target != null -> textAttributesKeyForPropertyDeclaration(target)
                else -> null
            }?.let { attribute ->
                highlightName(expression, attribute)
                if (target?.isMutableVariable() == true || targetSymbol != null && isBackingFieldReferencingMutableVariable(targetSymbol)) {
                    highlightName(expression, Colors.MUTABLE_VARIABLE)
                }
            }
        }
    }

    @Suppress("unused")
    private fun KtAnalysisSession.isBackingFieldReferencingMutableVariable(symbol: KtSymbol): Boolean {
        if (symbol !is KtBackingFieldSymbol) return false
        return !symbol.owningProperty.isVal
    }

    private fun KtSimpleNameExpression.isByNameArgumentReference() =
        parent is KtValueArgumentName


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

