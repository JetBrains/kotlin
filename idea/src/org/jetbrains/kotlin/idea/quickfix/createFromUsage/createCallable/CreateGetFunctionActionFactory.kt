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
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.ParameterInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.*

object CreateGetFunctionActionFactory : CreateGetSetFunctionActionFactory(isGet = true) {
    override fun createCallableInfo(element: KtArrayAccessExpression, diagnostic: Diagnostic): CallableInfo? {
        val arrayExpr = element.arrayExpression ?: return null

        val arrayType = TypeInfo(arrayExpr, Variance.IN_VARIANCE)
        val parameters = element.indexExpressions.map { ParameterInfo(TypeInfo(it, Variance.IN_VARIANCE)) }
        val returnType = TypeInfo(element, Variance.OUT_VARIANCE)
        return FunctionInfo(
                OperatorNameConventions.GET.asString(), arrayType, returnType, Collections.emptyList(), parameters, isOperator = true
        )
    }
}
