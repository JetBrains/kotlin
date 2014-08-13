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

package org.jetbrains.jet.plugin.util

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.tree.IElementType
import org.jetbrains.jet.lang.parsing.JetExpressionParsing
import java.util.HashMap
import org.jetbrains.jet.lang.parsing.JetExpressionParsing.Precedence.*
import org.jetbrains.jet.lang.psi.*

public object JetPsiPrecedences {

    private val LOG = Logger.getInstance(javaClass<JetPsiPrecedences>())

    private val precedence: Map<IElementType, Int>
    {
        val builder = HashMap<IElementType, Int>()
        for ((i, record) in JetExpressionParsing.Precedence.values().withIndices()) {
            for (elementType in record.getOperations().getTypes()) {
                builder[elementType] = i
            }
        }
        precedence = builder
    }

    public val PRECEDENCE_OF_ATOMIC_EXPRESSION: Int = -1
    public val PRECEDENCE_OF_PREFIX_EXPRESSION: Int = PREFIX.ordinal()
    public val PRECEDENCE_OF_POSTFIX_EXPRESSION: Int = POSTFIX.ordinal()

    public fun getPrecedence(expression: JetExpression): Int {
        return when (expression) {
            is JetAnnotatedExpression,
            is JetLabeledExpression,
            is JetPrefixExpression -> PRECEDENCE_OF_PREFIX_EXPRESSION
            is JetPostfixExpression -> PRECEDENCE_OF_POSTFIX_EXPRESSION
            is JetOperationExpression -> {
                val operation = expression.getOperationReference().getReferencedNameElementType()
                val precedenceNumber = precedence[operation]
                if (precedenceNumber == null) {
                    LOG.error("No precedence for operation: " + operation)
                    precedence.size()
                }
                else precedenceNumber
            }
            else -> PRECEDENCE_OF_ATOMIC_EXPRESSION
        }
    }

    public fun isTighter(subject: Int, tighterThan: Int): Boolean {
        return subject < tighterThan
    }
}
