/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.utils.addToStdlib.popLast

inline fun <T> isCallTheFirstStatementForPsi(
    root: T,
    getElementType: (T) -> IElementType,
    getChildren: (T) -> List<T>,
): Boolean {
    val stack = getChildren(root).asReversed().toMutableList()

    while (stack.isNotEmpty()) {
        val child = stack.popLast()
        when (getElementType(child)) {
            org.jetbrains.kotlin.lexer.KtTokens.LBRACE, org.jetbrains.kotlin.lexer.KtTokens.WHITE_SPACE, org.jetbrains.kotlin.lexer.KtTokens.DOT, org.jetbrains.kotlin.lexer.KtTokens.EOL_COMMENT -> {}
            org.jetbrains.kotlin.KtNodeTypes.CALL_EXPRESSION -> return true
            org.jetbrains.kotlin.KtNodeTypes.REFERENCE_EXPRESSION -> {}
            org.jetbrains.kotlin.KtNodeTypes.DOT_QUALIFIED_EXPRESSION -> {
                stack += getChildren(child).asReversed()
            }
            org.jetbrains.kotlin.KtNodeTypes.ANNOTATION_ENTRY -> {}
            org.jetbrains.kotlin.KtNodeTypes.ANNOTATED_EXPRESSION -> {
                stack += getChildren(child).asReversed()
            }
            else -> return false
        }
    }
    return false
}

inline fun <T> firstFunctionCallInBlockHasLambdaArgumentWithLabelForPsi(
    root: T,
    getElementType: (T) -> IElementType,
    getChildren: (T) -> List<T>,
): Boolean {
    val functionCall = getChildren(root).firstOrNull { getElementType(it) == org.jetbrains.kotlin.KtNodeTypes.CALL_EXPRESSION } ?: return false
    val lambda = getChildren(functionCall).firstOrNull { getElementType(it) == org.jetbrains.kotlin.KtNodeTypes.LAMBDA_ARGUMENT } ?: return false
    val expr = getChildren(lambda).singleOrNull() ?: return false
    return getElementType(expr) == org.jetbrains.kotlin.KtNodeTypes.LABELED_EXPRESSION
}
