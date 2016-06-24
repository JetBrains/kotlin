/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.ErrorUtils

class InfixCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, context: BasicCallResolutionContext) {
        val functionDescriptor = resolvedCall.resultingDescriptor as? FunctionDescriptor ?: return
        if (functionDescriptor.isDynamic() || ErrorUtils.isError(functionDescriptor)) return
        val element = ((resolvedCall as? VariableAsFunctionResolvedCall)?.variableCall ?: resolvedCall).call.calleeExpression
        if (isInfixCall(element) && !functionDescriptor.isInfix) {
            val operationRefExpression = element as? KtOperationReferenceExpression ?: return
            val containingDeclarationName = functionDescriptor.containingDeclaration.fqNameUnsafe.asString()
            context.trace.report(Errors.INFIX_MODIFIER_REQUIRED.on(operationRefExpression, functionDescriptor, containingDeclarationName))
        }
    }

    companion object {
        fun isInfixCall(element: PsiElement?): Boolean {
            val operationRefExpression = element as? KtOperationReferenceExpression ?: return false
            val binaryExpression = operationRefExpression.parent as? KtBinaryExpression ?: return false
            return binaryExpression.operationReference === operationRefExpression && !operationRefExpression.isPredefinedOperator()
        }
    }
}
