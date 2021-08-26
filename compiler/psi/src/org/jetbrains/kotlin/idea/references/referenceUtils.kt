/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.references.ReferenceAccess
import org.jetbrains.kotlin.utils.addToStdlib.constant
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

val KtSimpleNameExpression.mainReference: KtSimpleNameReference
    get() = references.firstIsInstance()

val KtReferenceExpression.mainReference: KtReference
    get() = if (this is KtSimpleNameExpression) mainReference else references.firstIsInstance()

val KDocName.mainReference: KDocReference
    get() = references.firstIsInstance()

val KtElement.mainReference: KtReference?
    get() = when (this) {
        is KtReferenceExpression -> mainReference
        is KDocName -> mainReference
        else -> references.firstIsInstanceOrNull()
    }

fun KtExpression.readWriteAccess(useResolveForReadWrite: Boolean) =
    readWriteAccessWithFullExpression(useResolveForReadWrite).first

fun KtExpression.readWriteAccessWithFullExpression(
    useResolveForReadWrite: Boolean,
    readWriteAccessWithFullExpressionByResolve: (KtBinaryExpression) -> Pair<ReferenceAccess, KtExpression>? = { null },
): Pair<ReferenceAccess, KtExpression> {
    var expression = getQualifiedExpressionForSelectorOrThis()
    loop@ while (true) {
        when (val parent = expression.parent) {
            is KtParenthesizedExpression, is KtAnnotatedExpression, is KtLabeledExpression -> expression = parent as KtExpression
            else -> break@loop
        }
    }

    val assignment = expression.getAssignmentByLHS()
    if (assignment != null) {
        return when (assignment.operationToken) {
            KtTokens.EQ -> ReferenceAccess.WRITE to assignment

            else -> {
                (if (useResolveForReadWrite) readWriteAccessWithFullExpressionByResolve(assignment) else null)
                    ?: (ReferenceAccess.READ_WRITE to assignment)
            }
        }
    }

    val unaryExpression = expression.parent as? KtUnaryExpression
    return if (unaryExpression != null && unaryExpression.operationToken in constant { setOf(KtTokens.PLUSPLUS, KtTokens.MINUSMINUS) })
        ReferenceAccess.READ_WRITE to unaryExpression
    else
        ReferenceAccess.READ to expression
}
