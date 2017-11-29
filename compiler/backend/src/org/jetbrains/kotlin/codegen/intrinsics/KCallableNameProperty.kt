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

package org.jetbrains.kotlin.codegen.intrinsics

import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.JAVA_STRING_TYPE
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.Type.VOID_TYPE

class KCallableNameProperty : IntrinsicPropertyGetter() {
    override fun generate(resolvedCall: ResolvedCall<*>?, codegen: ExpressionCodegen, returnType: Type, receiver: StackValue): StackValue? {
        val expressionReceiver = resolvedCall!!.dispatchReceiver as? ExpressionReceiver ?: return null
        val expression = expressionReceiver.expression as? KtCallableReferenceExpression ?: return null

        val referenceResolvedCall = expression.callableReference.getResolvedCall(codegen.bindingContext) ?: return null
        val callableReferenceReceiver = JvmCodegenUtil.getBoundCallableReferenceReceiver(referenceResolvedCall)

        return StackValue.operation(returnType) { iv ->
            // Generate the left-hand side of a bound callable reference expression
            if (callableReferenceReceiver != null && callableReferenceReceiver !is ImplicitClassReceiver) {
                val stackValue = codegen.generateReceiverValue(callableReferenceReceiver, false)
                StackValue.coercion(stackValue, codegen.asmType(callableReferenceReceiver.type)).put(VOID_TYPE, iv)
            }

            iv.aconst(referenceResolvedCall.resultingDescriptor.name.asString())
            StackValue.coerce(JAVA_STRING_TYPE, returnType, iv)
        }
    }
}
