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

package org.jetbrains.kotlin.idea.util

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.parsing.KotlinExpressionParsing
import org.jetbrains.kotlin.parsing.KotlinExpressionParsing.Precedence.POSTFIX
import org.jetbrains.kotlin.parsing.KotlinExpressionParsing.Precedence.PREFIX
import org.jetbrains.kotlin.psi.*
import java.util.*

object PsiPrecedences {

    private val LOG = Logger.getInstance(PsiPrecedences::class.java)

    private val precedence: Map<IElementType, Int>
    init {
        val builder = HashMap<IElementType, Int>()
        for ((i, record) in KotlinExpressionParsing.Precedence.values().withIndex()) {
            for (elementType in record.operations.types) {
                builder[elementType] = i
            }
        }
        precedence = builder
    }

    val PRECEDENCE_OF_ATOMIC_EXPRESSION: Int = -1
    val PRECEDENCE_OF_PREFIX_EXPRESSION: Int = PREFIX.ordinal
    val PRECEDENCE_OF_POSTFIX_EXPRESSION: Int = POSTFIX.ordinal

    fun getPrecedence(expression: KtExpression): Int {
        return when (expression) {
            is KtAnnotatedExpression,
            is KtLabeledExpression,
            is KtPrefixExpression -> PRECEDENCE_OF_PREFIX_EXPRESSION
            is KtPostfixExpression -> PRECEDENCE_OF_POSTFIX_EXPRESSION
            is KtOperationExpression -> {
                val operation = expression.operationReference.getReferencedNameElementType()
                val precedenceNumber = precedence[operation]
                if (precedenceNumber == null) {
                    LOG.error("No precedence for operation: " + operation)
                    precedence.size
                }
                else precedenceNumber
            }
            else -> PRECEDENCE_OF_ATOMIC_EXPRESSION
        }
    }

    fun isTighter(subject: Int, tighterThan: Int): Boolean {
        return subject < tighterThan
    }
}
