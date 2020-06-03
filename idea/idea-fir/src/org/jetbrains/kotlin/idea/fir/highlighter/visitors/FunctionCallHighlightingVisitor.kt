/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.highlighter.visitors

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.editor.colors.TextAttributesKey
import org.jetbrains.kotlin.idea.frontend.api.*
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.serialization.deserialization.KOTLIN_SUSPEND_BUILT_IN_FUNCTION_FQ_NAME
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors as Colors

internal class FunctionCallHighlightingVisitor(
    analysisSession: FrontendAnalysisSession,
    holder: AnnotationHolder
) : FirAfterResolveHighlightingVisitor(analysisSession, holder) {
    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        (expression.operationReference as? KtReferenceExpression)
            ?.takeIf {
                // do not highlight assignment statement
                (it as? KtOperationReferenceExpression)?.operationSignTokenType != KtTokens.EQ
            }?.let { callee ->
                analysisSession.resolveCall(expression)
                    ?.takeIf { callInfo ->
                        // ignore arithmetic-like operator calls
                        (callInfo.targetFunction as? KtNamedFunction)?.hasModifier(KtTokens.OPERATOR_KEYWORD) != true
                    }
                    ?.let { callInfo ->
                        getTextAttributesForCal(callInfo)?.let { attributes ->
                            highlightName(callee, attributes)
                        }
                    }
            }
        super.visitBinaryExpression(expression)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        expression.calleeExpression
            ?.takeUnless { it is KtLambdaExpression }
            ?.takeUnless { it is KtCallExpression /* KT-16159 */}
            ?.let { callee ->
                analysisSession.resolveCall(expression)?.let { callInfo ->
                    getTextAttributesForCal(callInfo)?.let { attributes ->
                        highlightName(callee, attributes)
                    }
                }
            }
        super.visitCallExpression(expression)
    }

    private fun getTextAttributesForCal(callInfo: CallInfo): TextAttributesKey? = when {
        callInfo.isSuspendCall -> Colors.SUSPEND_FUNCTION_CALL
        callInfo is ConstructorCallInfo ->
            Colors.CONSTRUCTOR_CALL
        callInfo is SimpleKtFunctionCallInfo -> when {
            callInfo.targetFunction.getKotlinFqName() == KOTLIN_SUSPEND_BUILT_IN_FUNCTION_FQ_NAME ->Colors.KEYWORD
            callInfo.targetFunction.isExtensionDeclaration() -> Colors.EXTENSION_FUNCTION_CALL
            callInfo.targetFunction.parent is KtFile -> Colors.PACKAGE_FUNCTION_CALL
            else -> Colors.FUNCTION_CALL
        }
        callInfo is SimpleJavaFunctionCallInfo -> Colors.FUNCTION_CALL
        callInfo is VariableAsFunctionCallInfo -> Colors.VARIABLE_AS_FUNCTION_CALL
        callInfo is VariableAsFunctionLikeCallInfo -> Colors.VARIABLE_AS_FUNCTION_LIKE_CALL
        else -> null
    }
}