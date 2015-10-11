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

package org.jetbrains.kotlin.resolve.validation

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.psi.JetOperationReferenceExpression
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.ErrorUtils

public class InfixValidator : SymbolUsageValidator {

    override fun validateCall(
            resolvedCall: ResolvedCall<*>?,
            targetDescriptor: CallableDescriptor,
            trace: BindingTrace,
            element: PsiElement
    ) {
        val functionDescriptor = targetDescriptor as? FunctionDescriptor ?: return
        if (functionDescriptor.isDynamic() || ErrorUtils.isError(functionDescriptor)) return
        if (isInfixCall(element) && !functionDescriptor.isInfix) {
            val operationRefExpression = element as? JetOperationReferenceExpression ?: return
            val containingDeclarationName = functionDescriptor.containingDeclaration.fqNameUnsafe.asString()
            trace.report(Errors.INFIX_MODIFIER_REQUIRED.on(operationRefExpression, functionDescriptor, containingDeclarationName))
        }
    }

    companion object {
        fun isInfixCall(element: PsiElement?): Boolean {
            val operationRefExpression = element as? JetOperationReferenceExpression ?: return false
            val binaryExpression = operationRefExpression.parent as? JetBinaryExpression ?: return false
            return binaryExpression.operationReference === operationRefExpression && !operationRefExpression.isPredefinedOperator()
        }
    }
}