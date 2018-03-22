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
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.lang.AssertionError

interface KtQualifiedExpression : KtExpression {
    val receiverExpression: KtExpression
        get() = getExpression(false) ?: throw AssertionError("No receiver found: ${getElementTextWithContext()}")

    val selectorExpression: KtExpression?
        get() = getExpression(true)

    val operationTokenNode: ASTNode
        get() = node.findChildByType(KtTokens.OPERATIONS)!!

    val operationSign: KtSingleValueToken
        get() = operationTokenNode.elementType as KtSingleValueToken

    private fun KtQualifiedExpression.getExpression(afterOperation: Boolean): KtExpression? {
        return operationTokenNode.psi?.siblings(afterOperation, false)?.firstIsInstanceOrNull<KtExpression>()
    }
}
