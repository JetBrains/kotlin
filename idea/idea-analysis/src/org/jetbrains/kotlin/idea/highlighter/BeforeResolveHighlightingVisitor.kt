/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.jetbrains.kotlin.psi.KtExpressionWithLabel
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtValueArgument
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
        createInfoAnnotation(TextRange(argumentName.startOffset, eq.endOffset), null).textAttributes = KotlinHighlightingColors.NAMED_ARGUMENT
    }

    override fun visitExpressionWithLabel(expression: KtExpressionWithLabel) {
        val targetLabel = expression.getTargetLabel()
        if (targetLabel != null) {
            highlightName(targetLabel, KotlinHighlightingColors.LABEL)
        }
    }
}
