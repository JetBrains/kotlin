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

package org.jetbrains.kotlin.psi

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens

internal fun KtElement.deleteSemicolon() {
    if (this is KtEnumEntry) return

    val sibling = PsiTreeUtil.skipSiblingsForward(this, PsiWhiteSpace::class.java, PsiComment::class.java)
    if (sibling == null || sibling.node.elementType != KtTokens.SEMICOLON) return

    val lastSiblingToDelete = PsiTreeUtil.skipSiblingsForward(sibling, PsiWhiteSpace::class.java)?.prevSibling ?: sibling
    parent.deleteChildRange(nextSibling, lastSiblingToDelete)
}

fun KtExpression.unpackFunctionLiteral(allowParentheses: Boolean = false): KtLambdaExpression? {
    return when (this) {
        is KtLambdaExpression -> this
        is KtLabeledExpression -> baseExpression?.unpackFunctionLiteral(allowParentheses)
        is KtAnnotatedExpression -> baseExpression?.unpackFunctionLiteral(allowParentheses)
        is KtParenthesizedExpression -> if (allowParentheses) expression?.unpackFunctionLiteral(allowParentheses) else null
        else -> null
    }
}