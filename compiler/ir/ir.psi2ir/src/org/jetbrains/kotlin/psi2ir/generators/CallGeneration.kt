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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.intermediate.*
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.scopes.receivers.*

fun StatementGenerator.generateReceiverOrNull(ktDefaultElement: KtElement, receiver: ReceiverValue?): IntermediateValue? =
        receiver?.let { generateReceiver(ktDefaultElement, receiver) }

fun StatementGenerator.generateReceiver(ktDefaultElement: KtElement, receiver: ReceiverValue): IntermediateValue {
    if (receiver is TransientReceiver) {
        return TransientReceiverValue(ktDefaultElement.text, receiver.type)
    }

    val receiverExpression = when (receiver) {
        is ImplicitClassReceiver ->
            IrThisReferenceImpl(ktDefaultElement.startOffset, ktDefaultElement.startOffset, receiver.type, receiver.classDescriptor)
        is ThisClassReceiver ->
            (receiver as? ExpressionReceiver)?.expression?.let { receiverExpression ->
                IrThisReferenceImpl(receiverExpression.startOffset, receiverExpression.endOffset, receiver.type,
                                    receiver.classDescriptor)
            } ?: TODO("Non-implicit ThisClassReceiver should be an expression receiver")
        is ExpressionReceiver ->
            generateExpression(receiver.expression)
        is ClassValueReceiver ->
            IrGetObjectValueImpl(receiver.expression.startOffset, receiver.expression.endOffset, receiver.type,
                                 receiver.classQualifier.descriptor)
        is ExtensionReceiver ->
            IrGetExtensionReceiverImpl(ktDefaultElement.startOffset, ktDefaultElement.startOffset, receiver.type,
                                       receiver.declarationDescriptor.extensionReceiverParameter!!)
        else ->
            TODO("Receiver: ${receiver.javaClass.simpleName}")
    }

    return if (receiverExpression is IrExpressionWithCopy)
        RematerializableValue(receiverExpression)
    else
        OnceExpressionValue(receiverExpression)
}

fun StatementGenerator.generateCallReceiver(
        ktDefaultElement: KtElement,
        dispatchReceiver: ReceiverValue?,
        extensionReceiver: ReceiverValue?,
        isSafe: Boolean
) : CallReceiver {
    val dispatchReceiverValue = generateReceiverOrNull(ktDefaultElement, dispatchReceiver)
    val extensionReceiverValue = generateReceiverOrNull(ktDefaultElement, extensionReceiver)

    if (!isSafe) {
        return SimpleCallReceiver(dispatchReceiverValue, extensionReceiverValue)
    }
    else if (extensionReceiverValue != null) {
        return SafeCallReceiver(this, ktDefaultElement.startOffset, ktDefaultElement.endOffset,
                                extensionReceiverValue.load(), dispatchReceiverValue)
    }
    else if (dispatchReceiverValue != null) {
        return SafeCallReceiver(this, ktDefaultElement.startOffset, ktDefaultElement.endOffset,
                                dispatchReceiverValue.load(), null)
    }
    else {
        return throw AssertionError("Safe call should have an explicit receiver: ${ktDefaultElement.text}")
    }
}

fun StatementGenerator.generateValueArgument(valueArgument: ResolvedValueArgument): IrExpression? =
        when (valueArgument) {
            is DefaultValueArgument ->
                null
            is ExpressionValueArgument ->
                generateExpression(valueArgument.valueArgument!!.getArgumentExpression()!!)
            is VarargValueArgument ->
                createDummyExpression(valueArgument.arguments[0].getArgumentExpression()!!, "vararg")
            else ->
                TODO("Unexpected valueArgument: ${valueArgument.javaClass.simpleName}")
        }

fun StatementGenerator.pregenerateCall(resolvedCall: ResolvedCall<*>): CallBuilder {
    val call = pregenerateCallReceivers(resolvedCall)

    resolvedCall.valueArgumentsByIndex!!.forEachIndexed { index, valueArgument ->
        call.irValueArgumentsByIndex[index] = generateValueArgument(valueArgument)
    }

    return call
}

fun StatementGenerator.pregenerateCallReceivers(resolvedCall: ResolvedCall<*>): CallBuilder {
    val call = CallBuilder(resolvedCall)

    call.callReceiver = generateCallReceiver(resolvedCall.call.callElement,
                                             resolvedCall.dispatchReceiver,
                                             resolvedCall.extensionReceiver,
                                             resolvedCall.call.isSafeCall())

    return call
}