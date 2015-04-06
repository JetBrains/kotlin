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
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.psi.JetArrayAccessExpression
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import java.util.Collections
import org.jetbrains.kotlin.idea.quickfix.JetIntentionActionsFactory

object CreateGetFunctionActionFactory : JetIntentionActionsFactory() {
    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction>? {
        val accessExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetArrayAccessExpression>()) ?: return null
        val arrayExpr = accessExpr.getArrayExpression() ?: return null
        val arrayType = TypeInfo(arrayExpr, Variance.IN_VARIANCE)

        val parameters = accessExpr.getIndexExpressions().map {
            ParameterInfo(TypeInfo(it, Variance.IN_VARIANCE))
        }

        val returnType = TypeInfo(accessExpr, Variance.OUT_VARIANCE)
        return CreateCallableFromUsageFixes(accessExpr, FunctionInfo("get", arrayType, returnType, Collections.emptyList(), parameters))
    }
}
