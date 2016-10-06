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

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCallableReference
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrGetClass
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext.DOUBLE_COLON_LHS
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.JAVA_STRING_TYPE
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.Type.VOID_TYPE
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class KCallableNameProperty : IntrinsicMethod() {

    override fun toCallable(expression: IrMemberAccessExpression, signature: JvmMethodSignature, context: JvmBackendContext): IrIntrinsicFunction {
        return object: IrIntrinsicFunction(expression, signature, context) {
            override fun invoke(v: InstructionAdapter, codegen: org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen, data: BlockInfo): StackValue {
                val callableReference = expression.dispatchReceiver as IrCallableReference
                /*TODO optimize generation*/

                codegen.gen(callableReference.dispatchReceiver ?: callableReference.extensionReceiver!!, VOID_TYPE, data)
                codegen.mv.aconst(callableReference.descriptor.name.asString())
                StackValue.coerce(JAVA_STRING_TYPE, returnType, codegen.mv)
                return StackValue.onStack(signature.returnType)
            }
        }
    }

    fun generate(resolvedCall: ResolvedCall<*>?, codegen: ExpressionCodegen, returnType: Type, receiver: StackValue): StackValue? {
        val expressionReceiver = resolvedCall!!.dispatchReceiver as? ExpressionReceiver ?: return null
        val expression = expressionReceiver.expression as? KtCallableReferenceExpression ?: return null

        val receiverExpression = expression.receiverExpression
        val lhs = receiverExpression?.let { codegen.bindingContext.get(DOUBLE_COLON_LHS, it) }

        val callableReference = expression.callableReference
        val descriptor = callableReference.getResolvedCall(codegen.bindingContext)?.resultingDescriptor ?: return null

        return StackValue.operation(returnType) { iv ->
            // Generate the left-hand side of a bound callable reference expression
            if (lhs is DoubleColonLHS.Expression) {
                codegen.gen(receiverExpression, VOID_TYPE)
            }
            iv.aconst(descriptor.name.asString())
            StackValue.coerce(JAVA_STRING_TYPE, returnType, iv)
        }
    }
}
