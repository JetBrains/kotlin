/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
                val kotlinType = callableReferenceReceiver.type
                StackValue.coercion(stackValue, codegen.asmType(kotlinType), kotlinType).put(VOID_TYPE, iv)
            }

            iv.aconst(referenceResolvedCall.resultingDescriptor.name.asString())
            StackValue.coerce(JAVA_STRING_TYPE, returnType, iv)
        }
    }
}
