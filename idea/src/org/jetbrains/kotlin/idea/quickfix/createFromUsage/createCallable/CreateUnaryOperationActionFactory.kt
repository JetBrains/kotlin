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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.FunctionInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.OperatorConventions

object CreateUnaryOperationActionFactory: CreateCallableMemberFromUsageFactory<KtUnaryExpression>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtUnaryExpression? {
        return diagnostic.psiElement.parent as? KtUnaryExpression
    }

    override fun createCallableInfo(element: KtUnaryExpression, diagnostic: Diagnostic): CallableInfo? {
        val token = element.operationToken as KtToken
        val operationName = OperatorConventions.getNameForOperationSymbol(token, true, false) ?: return null
        val incDec = token in OperatorConventions.INCREMENT_OPERATIONS

        val receiverExpr = element.baseExpression ?: return null

        val receiverType = TypeInfo(receiverExpr, Variance.IN_VARIANCE)
        val returnType = if (incDec) TypeInfo.ByReceiverType(Variance.OUT_VARIANCE) else TypeInfo(element, Variance.OUT_VARIANCE)
        return FunctionInfo(operationName.asString(), receiverType, returnType, isOperator = true)
    }
}
