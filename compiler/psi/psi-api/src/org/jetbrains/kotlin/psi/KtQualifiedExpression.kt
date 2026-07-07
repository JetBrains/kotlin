/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolution.KtResolvableCall
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

/**
 * Represents a qualified expression, which accesses a member or extension
 * on a receiver using the `.` or `?.` operator.
 *
 * [receiverExpression] is the left-hand side, [selectorExpression] is the right-hand side,
 * and [operationSign] is the operator token (`.` or `?.`).
 *
 * ### Examples:
 *
 * ```kotlin
 * val len = str.length
 * //        ^________^
 * ```
 *
 * ```kotlin
 * val len = str?.length
 * //        ^_________^
 * ```
 *
 * @see KtDotQualifiedExpression
 * @see KtSafeQualifiedExpression
 */
@OptIn(KtExperimentalApi::class)
interface KtQualifiedExpression : KtExpression, KtResolvableCall {
    /**
     * The receiver on the left-hand side of the `.` or `?.` operator. Throws if the receiver is missing; use
     * [receiverExpressionOrNull] when the PSI may be inconsistent.
     */
    val receiverExpression: KtExpression
        @OptIn(KtPsiInconsistencyHandling::class)
        get() = receiverExpressionOrNull ?: errorWithAttachment("No receiver found in qualified expression") {
            withPsiEntry("qualifiedExpression", this@KtQualifiedExpression)
        }

    /**
     * A consistent [KtQualifiedExpression] should always have a receiver, so [receiverExpression] should be preferred if possible. Only use
     * [receiverExpressionOrNull] if you suspect that the PSI might be inconsistent (e.g. due to ongoing modification) and need a `null`
     * value instead of an error.
     */
    @KtPsiInconsistencyHandling
    val receiverExpressionOrNull: KtExpression?
        get() = getExpression(false)

    /**
     * The selector on the right-hand side of the operator (the member or extension being accessed), or `null` if it is
     * absent in incomplete code.
     */
    val selectorExpression: KtExpression?
        get() = getExpression(true)

    /**
     * The AST node of the qualification operator (`.` or `?.`). Throws if it is missing.
     */
    val operationTokenNode: ASTNode
        get() = operationTokenNodeOrNull ?: error(
            "No operation node for ${node.elementType}. Children: ${children.contentToString()}"
        )

    private val operationTokenNodeOrNull: ASTNode?
        get() = node.findChildByType(KtTokens.OPERATIONS)

    /**
     * The qualification operator token: [KtTokens.DOT] for `.` or [KtTokens.SAFE_ACCESS] for `?.`.
     */
    val operationSign: KtSingleValueToken
        get() = operationTokenNode.elementType as KtSingleValueToken

    private fun getExpression(afterOperation: Boolean): KtExpression? {
        return operationTokenNodeOrNull?.psi?.siblings(afterOperation, false)?.firstIsInstanceOrNull<KtExpression>()
    }
}
