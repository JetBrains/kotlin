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
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.FunctionInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.ParameterInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtOperationExpression
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.*

object CreateSetFunctionActionFactory : CreateGetSetFunctionActionFactory(isGet = false) {
    override fun createCallableInfo(element: KtArrayAccessExpression, diagnostic: Diagnostic): CallableInfo? {
        val arrayExpr = element.arrayExpression ?: return null
        val arrayType = TypeInfo(arrayExpr, Variance.IN_VARIANCE)

        val builtIns = element.builtIns

        val parameters = element.indexExpressions.mapTo(ArrayList<ParameterInfo>()) {
            ParameterInfo(TypeInfo(it, Variance.IN_VARIANCE))
        }
        val assignmentExpr = QuickFixUtil.getParentElementOfType(diagnostic, KtOperationExpression::class.java) ?: return null
        val valType = when (assignmentExpr) {
            is KtBinaryExpression -> {
                TypeInfo(assignmentExpr.right ?: return null, Variance.IN_VARIANCE)
            }
            is KtUnaryExpression -> {
                if (assignmentExpr.operationToken !in OperatorConventions.INCREMENT_OPERATIONS) return null

                val context = assignmentExpr.analyze()
                val rhsType = assignmentExpr.getResolvedCall(context)?.resultingDescriptor?.returnType
                TypeInfo(if (rhsType == null || ErrorUtils.containsErrorType(rhsType)) builtIns.anyType else rhsType, Variance.IN_VARIANCE)
            }
            else -> return null
        }
        parameters.add(ParameterInfo(valType, "value"))

        val returnType = TypeInfo(builtIns.unitType, Variance.OUT_VARIANCE)
        return FunctionInfo(
                OperatorNameConventions.SET.asString(), arrayType, returnType, Collections.emptyList(), parameters, isOperator = true
        )
    }
}
