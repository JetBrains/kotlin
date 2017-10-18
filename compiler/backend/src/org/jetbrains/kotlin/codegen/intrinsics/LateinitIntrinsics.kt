/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type

object LateinitIsInitialized : IntrinsicPropertyGetter() {
    override fun generate(resolvedCall: ResolvedCall<*>?, codegen: ExpressionCodegen, returnType: Type, receiver: StackValue): StackValue? {
        val value = getStackValue(resolvedCall ?: return null, codegen) ?: return null
        return StackValue.compareWithNull(value, Opcodes.IFNULL)
    }
}

private fun getStackValue(resolvedCall: ResolvedCall<*>, codegen: ExpressionCodegen): StackValue? {
    val expression =
            (resolvedCall.extensionReceiver as? ExpressionReceiver)?.expression as? KtCallableReferenceExpression ?: return null
    val referenceResolvedCall = expression.callableReference.getResolvedCallWithAssert(codegen.bindingContext)

    val receiver = codegen.generateCallableReferenceReceiver(referenceResolvedCall) ?: StackValue.none()

    val target = expression.callableReference.getResolvedCallWithAssert(codegen.bindingContext).resultingDescriptor
    return codegen.intermediateValueForProperty(target as PropertyDescriptor, true, false, null, false, receiver, null, true)
}
