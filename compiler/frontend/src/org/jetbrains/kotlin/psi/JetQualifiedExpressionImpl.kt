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
import org.jetbrains.kotlin.lexer.JetToken
import org.jetbrains.kotlin.lexer.JetTokens
import com.intellij.psi.util.PsiTreeUtil

object JetQualifiedExpressionImpl {
    public fun JetQualifiedExpression.getOperationTokenNode(): ASTNode {
        val operationNode = this.getNode()!!.findChildByType(JetTokens.OPERATIONS)
        return operationNode!!
    }

    public fun JetQualifiedExpression.getOperationSign(): JetToken {
        return this.getOperationTokenNode().getElementType() as JetToken
    }

    public fun JetQualifiedExpression.getReceiverExpression(): JetExpression {
        val left = PsiTreeUtil.findChildOfType(this, javaClass<JetExpression>())
        return left!!
    }

    public fun JetQualifiedExpression.getSelectorExpression(): JetExpression? {
        var node: ASTNode? = getOperationTokenNode()
        while (node != null) {
            val psi = node!!.getPsi()
            if (psi is JetExpression) {
                return (psi as JetExpression)
            }
            node = node!!.getTreeNext()
        }
        return null
    }
}
