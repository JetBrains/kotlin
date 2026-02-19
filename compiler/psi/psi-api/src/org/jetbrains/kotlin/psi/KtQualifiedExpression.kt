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

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry
import java.util.*

interface KtQualifiedExpression : KtExpression {
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

    val selectorExpression: KtExpression?
        get() = getExpression(true)

    val operationTokenNode: ASTNode
        get() = operationTokenNodeOrNull ?: error(
            "No operation node for ${node.elementType}. Children: ${Arrays.toString(children)}"
        )

    private val operationTokenNodeOrNull: ASTNode?
        get() = node.findChildByType(KtTokens.OPERATIONS)

    val operationSign: KtSingleValueToken
        get() = operationTokenNode.elementType as KtSingleValueToken

    private fun getExpression(afterOperation: Boolean): KtExpression? {
        return operationTokenNodeOrNull?.psi?.siblings(afterOperation, false)?.firstIsInstanceOrNull<KtExpression>()
    }
}
