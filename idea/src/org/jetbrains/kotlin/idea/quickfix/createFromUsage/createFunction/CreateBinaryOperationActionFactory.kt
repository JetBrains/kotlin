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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createFunction

import org.jetbrains.kotlin.idea.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.kotlin.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.lexer.JetToken
import org.jetbrains.kotlin.types.Variance
import java.util.Collections
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*

public object CreateBinaryOperationActionFactory: JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val callExpr = diagnostic.getPsiElement().getParent() as? JetBinaryExpression ?: return null
        val token = callExpr.getOperationToken() as JetToken
        val operationName = when (token) {
            JetTokens.IDENTIFIER -> callExpr.getOperationReference().getReferencedName()
            else -> OperatorConventions.getNameForOperationSymbol(token)?.asString()
        } ?: return null
        val inOperation = token in OperatorConventions.IN_OPERATIONS
        val comparisonOperation = token in OperatorConventions.COMPARISON_OPERATIONS

        val leftExpr = callExpr.getLeft() ?: return null
        val rightExpr = callExpr.getRight() ?: return null

        val receiverExpr = if (inOperation) rightExpr else leftExpr
        val argumentExpr = if (inOperation) leftExpr else rightExpr

        val builtIns = KotlinBuiltIns.getInstance()

        val receiverType = TypeInfo(receiverExpr, Variance.IN_VARIANCE)
        val returnType = when {
            inOperation -> TypeInfo.ByType(builtIns.getBooleanType(), Variance.INVARIANT).noSubstitutions()
            comparisonOperation -> TypeInfo.ByType(builtIns.getIntType(), Variance.INVARIANT).noSubstitutions()
            else -> TypeInfo(callExpr, Variance.OUT_VARIANCE)
        }
        val parameters = Collections.singletonList(ParameterInfo(TypeInfo(argumentExpr, Variance.IN_VARIANCE)))
        return CreateCallableFromUsageFix(callExpr, FunctionInfo(operationName, receiverType, returnType, Collections.emptyList(), parameters))
    }
}
