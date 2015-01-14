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
import org.jetbrains.kotlin.idea.quickfix.QuickFixUtil
import org.jetbrains.kotlin.psi.JetArrayAccessExpression
import org.jetbrains.kotlin.types.Variance
import java.util.ArrayList
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import java.util.Collections
import org.jetbrains.kotlin.psi.JetOperationExpression
import org.jetbrains.kotlin.psi.JetUnaryExpression
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.idea.caches.resolve.analyze

object CreateSetFunctionActionFactory : JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val accessExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetArrayAccessExpression>()) ?: return null
        val arrayExpr = accessExpr.getArrayExpression() ?: return null
        val arrayType = TypeInfo(arrayExpr, Variance.IN_VARIANCE)

        val builtIns = KotlinBuiltIns.getInstance()

        val parameters = accessExpr.getIndexExpressions().mapTo(ArrayList<ParameterInfo>()) {
            ParameterInfo(TypeInfo(it, Variance.IN_VARIANCE))
        }

        val assignmentExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetOperationExpression>()) ?: return null
        val valType = when (assignmentExpr) {
            is JetBinaryExpression -> {
                TypeInfo(assignmentExpr.getRight() ?: return null, Variance.IN_VARIANCE)
            }
            is JetUnaryExpression -> {
                if (assignmentExpr.getOperationToken() !in OperatorConventions.INCREMENT_OPERATIONS) return null

                val context = assignmentExpr.analyze()
                val rhsType = assignmentExpr.getResolvedCall(context)?.getResultingDescriptor()?.getReturnType()
                TypeInfo(if (rhsType == null || ErrorUtils.containsErrorType(rhsType)) builtIns.getAnyType() else rhsType, Variance.IN_VARIANCE)
            }
            else -> return null
        }
        parameters.add(ParameterInfo(valType, "value"))

        val returnType = TypeInfo(builtIns.getUnitType(), Variance.OUT_VARIANCE)
        return CreateCallableFromUsageFix(accessExpr, FunctionInfo("set", arrayType, returnType, Collections.emptyList(), parameters))
    }
}
