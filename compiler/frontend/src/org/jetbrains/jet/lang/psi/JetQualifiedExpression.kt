/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.Nullable
import org.jetbrains.jet.lexer.JetToken
import org.jetbrains.jet.lexer.JetTokens

public trait JetQualifiedExpression : JetExpression {
    public fun getReceiverExpression(): JetExpression

    public fun getSelectorExpression(): JetExpression?

    public fun getOperationTokenNode(): ASTNode {
        val operationNode = getNode()!!.findChildByType(JetTokens.OPERATIONS)
        return operationNode!!
    }

    public fun getOperationSign(): JetToken {
        return getOperationTokenNode().getElementType() as JetToken
    }
}

public abstract class JetQualifiedExpressionImpl(node: ASTNode) : JetExpressionImpl(node), JetQualifiedExpression {
    public override fun getReceiverExpression(): JetExpression {
        val left = findChildByClass(javaClass<JetExpression>())
        return left!!
    }

    public override fun getSelectorExpression(): JetExpression? {
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
