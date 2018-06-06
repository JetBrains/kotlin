/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import java.util.*

object CreateBinaryOperationActionFactory : CreateCallableMemberFromUsageFactory<KtBinaryExpression>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtBinaryExpression? {
        return diagnostic.psiElement.parent as? KtBinaryExpression
    }

    override fun createCallableInfo(element: KtBinaryExpression, diagnostic: Diagnostic): CallableInfo? {
        val token = element.operationToken as KtToken
        val operationName = when (token) {
                                KtTokens.IDENTIFIER -> element.operationReference.getReferencedName()
                                else -> OperatorConventions.getNameForOperationSymbol(token, false, true)?.asString()
                            } ?: return null
        val inOperation = token in OperatorConventions.IN_OPERATIONS
        val comparisonOperation = token in OperatorConventions.COMPARISON_OPERATIONS

        val leftExpr = element.left ?: return null
        val rightExpr = element.right ?: return null

        val receiverExpr = if (inOperation) rightExpr else leftExpr
        val argumentExpr = if (inOperation) leftExpr else rightExpr

        val builtIns = element.builtIns
        val receiverType = TypeInfo(receiverExpr, Variance.IN_VARIANCE)
        val returnType = when {
            inOperation -> TypeInfo.ByType(builtIns.booleanType, Variance.INVARIANT).noSubstitutions()
            comparisonOperation -> TypeInfo.ByType(builtIns.intType, Variance.INVARIANT).noSubstitutions()
            else -> TypeInfo(element, Variance.OUT_VARIANCE)
        }
        val parameters = Collections.singletonList(ParameterInfo(TypeInfo(argumentExpr, Variance.IN_VARIANCE)))
        val isOperator = token != KtTokens.IDENTIFIER
        return FunctionInfo(operationName, receiverType, returnType, parameterInfos = parameters,
                            isOperator = isOperator,
                            isInfix = !isOperator)
    }
}
