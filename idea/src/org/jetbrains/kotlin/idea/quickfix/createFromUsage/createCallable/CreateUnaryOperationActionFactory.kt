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
import org.jetbrains.kotlin.lexer.JetToken
import org.jetbrains.kotlin.psi.JetUnaryExpression
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.OperatorConventions

public object CreateUnaryOperationActionFactory: CreateCallableMemberFromUsageFactory<JetUnaryExpression>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): JetUnaryExpression? {
        return diagnostic.psiElement.parent as? JetUnaryExpression
    }

    override fun createCallableInfo(element: JetUnaryExpression, diagnostic: Diagnostic): CallableInfo? {
        val token = element.operationToken as JetToken
        val operationName = OperatorConventions.getNameForOperationSymbol(token) ?: return null
        val incDec = token in OperatorConventions.INCREMENT_OPERATIONS

        val receiverExpr = element.baseExpression ?: return null

        val receiverType = TypeInfo(receiverExpr, Variance.IN_VARIANCE)
        val returnType = if (incDec) TypeInfo.ByReceiverType(Variance.OUT_VARIANCE) else TypeInfo(element, Variance.OUT_VARIANCE)
        return FunctionInfo(operationName.asString(), receiverType, returnType, isOperator = true)
    }
}
