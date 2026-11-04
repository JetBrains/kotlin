/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.syntax.BasicSyntaxElementTypeIds
import org.jetbrains.kotlin.syntax.KotlinSyntaxNodeTypeIds
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds
import org.jetbrains.kotlin.utils.addToStdlib.popLast

inline fun <T> isCallTheFirstStatement(
    root: T,
    getElementTokenId: (T) -> Int,
    getChildren: (T) -> List<T>,
): Boolean {
    val stack = getChildren(root).asReversed().toMutableList()

    while (stack.isNotEmpty()) {
        val child = stack.popLast()
        when (getElementTokenId(child)) {
            KotlinSyntaxTokenIds.LBRACE, BasicSyntaxElementTypeIds.WHITE_SPACE, KotlinSyntaxTokenIds.DOT, KotlinSyntaxTokenIds.EOL_COMMENT -> {}
            KotlinSyntaxNodeTypeIds.CALL_EXPRESSION -> return true
            KotlinSyntaxNodeTypeIds.REFERENCE_EXPRESSION -> {}
            KotlinSyntaxNodeTypeIds.DOT_QUALIFIED_EXPRESSION -> {
                stack += getChildren(child).asReversed()
            }
            KotlinSyntaxNodeTypeIds.ANNOTATION_ENTRY -> {}
            KotlinSyntaxNodeTypeIds.ANNOTATED_EXPRESSION -> {
                stack += getChildren(child).asReversed()
            }
            else -> return false
        }
    }
    return false
}

inline fun <T> firstFunctionCallInBlockHasLambdaArgumentWithLabel(
    root: T,
    getElementTokenId: (T) -> Int,
    getChildren: (T) -> List<T>,
): Boolean {
    val functionCall = getChildren(root).firstOrNull { getElementTokenId(it) == KotlinSyntaxNodeTypeIds.CALL_EXPRESSION } ?: return false
    val lambda = getChildren(functionCall).firstOrNull { getElementTokenId(it) == KotlinSyntaxNodeTypeIds.LAMBDA_ARGUMENT } ?: return false
    val expr = getChildren(lambda).singleOrNull() ?: return false
    return getElementTokenId(expr) == KotlinSyntaxNodeTypeIds.LABELED_EXPRESSION
}
