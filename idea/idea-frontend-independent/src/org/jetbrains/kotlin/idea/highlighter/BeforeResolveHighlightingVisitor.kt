/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeHighlighting.RainbowHighlighter
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

internal class BeforeResolveHighlightingVisitor(holder: AnnotationHolder) : HighlightingVisitor(holder) {

    override fun visitElement(element: PsiElement) {
        val elementType = element.node.elementType
        val attributes = when {
            element is KDocLink && !willApplyRainbowHighlight(element) -> KotlinHighlightingColors.KDOC_LINK

            elementType in KtTokens.SOFT_KEYWORDS -> {
                when (elementType) {
                    in KtTokens.MODIFIER_KEYWORDS -> KotlinHighlightingColors.BUILTIN_ANNOTATION
                    else -> KotlinHighlightingColors.KEYWORD
                }
            }
            elementType == KtTokens.SAFE_ACCESS -> KotlinHighlightingColors.SAFE_ACCESS
            elementType == KtTokens.EXCLEXCL -> KotlinHighlightingColors.EXCLEXCL
            else -> return
        }

        createInfoAnnotation(element, null).textAttributes = attributes
    }

    private fun willApplyRainbowHighlight(element: KDocLink): Boolean {
        if (!RainbowHighlighter.isRainbowEnabledWithInheritance(EditorColorsManager.getInstance().globalScheme, KotlinLanguage.INSTANCE)) {
            return false
        }
        // Can't use resolve because it will access indices
        return (element.parent as? KDocTag)?.knownTag == KDocKnownTag.PARAM
    }

    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        if (ApplicationManager.getApplication().isUnitTestMode) return

        val functionLiteral = lambdaExpression.functionLiteral
        createInfoAnnotation(functionLiteral.lBrace, null).textAttributes = KotlinHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW

        val closingBrace = functionLiteral.rBrace
        if (closingBrace != null) {
            createInfoAnnotation(closingBrace, null).textAttributes = KotlinHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW
        }

        val arrow = functionLiteral.arrow
        if (arrow != null) {
            createInfoAnnotation(arrow, null).textAttributes = KotlinHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW
        }
    }

    override fun visitArgument(argument: KtValueArgument) {
        val argumentName = argument.getArgumentName() ?: return
        val eq = argument.equalsToken ?: return
        createInfoAnnotation(TextRange(argumentName.startOffset, eq.endOffset), null).textAttributes =
            if (argument.parent.parent is KtAnnotationEntry)
                KotlinHighlightingColors.ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES
            else
                KotlinHighlightingColors.NAMED_ARGUMENT
    }

    override fun visitExpressionWithLabel(expression: KtExpressionWithLabel) {
        val targetLabel = expression.getTargetLabel()
        if (targetLabel != null) {
            highlightName(targetLabel, KotlinHighlightingColors.LABEL)
        }
    }

    override fun visitSuperTypeCallEntry(call: KtSuperTypeCallEntry) {
        val calleeExpression = call.calleeExpression
        val typeElement = calleeExpression.typeReference?.typeElement
        if (typeElement is KtUserType) {
            typeElement.referenceExpression?.let { highlightName(it, KotlinHighlightingColors.CONSTRUCTOR_CALL) }
        }
        super.visitSuperTypeCallEntry(call)
    }


    override fun visitTypeParameter(parameter: KtTypeParameter) {
        parameter.nameIdentifier?.let { highlightName(it, KotlinHighlightingColors.TYPE_PARAMETER) }
        super.visitTypeParameter(parameter)
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        highlightNamedDeclaration(function, KotlinHighlightingColors.FUNCTION_DECLARATION)
        super.visitNamedFunction(function)
    }
}
