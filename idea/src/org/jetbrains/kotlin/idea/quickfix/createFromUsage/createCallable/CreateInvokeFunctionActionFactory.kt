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
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.FunctionInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.ParameterInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.util.OperatorNameConventions

object CreateInvokeFunctionActionFactory : CreateCallableMemberFromUsageFactory<KtCallExpression>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtCallExpression? {
        return diagnostic.psiElement.parent as? KtCallExpression
    }

    override fun createCallableInfo(element: KtCallExpression, diagnostic: Diagnostic): CallableInfo? {
        val expectedType = Errors.FUNCTION_EXPECTED.cast(diagnostic).b
        if (expectedType.isError) return null

        val receiverType = TypeInfo(expectedType, Variance.IN_VARIANCE)

        val anyType = element.builtIns.nullableAnyType
        val parameters = element.valueArguments.map {
            ParameterInfo(
                    it.getArgumentExpression()?.let { TypeInfo(it, Variance.IN_VARIANCE) } ?: TypeInfo(anyType, Variance.IN_VARIANCE),
                    it.getArgumentName()?.referenceExpression?.getReferencedName()
            )
        }

        val returnType = TypeInfo(element, Variance.OUT_VARIANCE)
        return FunctionInfo(OperatorNameConventions.INVOKE.asString(), receiverType, returnType, parameterInfos = parameters, isOperator = true)
    }
}
