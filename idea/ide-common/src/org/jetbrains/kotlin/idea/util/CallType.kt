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

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.psi.psiUtil.isImportDirectiveExpression
import org.jetbrains.kotlin.psi.psiUtil.isPackageDirectiveExpression

public sealed class CallType<TReceiver : JetElement?> {
    object DEFAULT : CallType<Nothing?>()

    object DOT : CallType<JetExpression>()

    object SAFE : CallType<JetExpression>()

    object INFIX : CallType<JetExpression>() {
        override fun canCall(descriptor: DeclarationDescriptor)
                = descriptor is SimpleFunctionDescriptor && descriptor.getValueParameters().size() == 1
    }

    object UNARY : CallType<JetExpression>() {
        override fun canCall(descriptor: DeclarationDescriptor)
                = descriptor is SimpleFunctionDescriptor && descriptor.getValueParameters().size() == 0
    }

    object CALLABLE_REFERENCE : CallType<JetTypeReference?>() {
        // currently callable references to locals and parameters are not supported
        override fun canCall(descriptor: DeclarationDescriptor)
                = descriptor is FunctionDescriptor || descriptor is PropertyDescriptor
    }

    //TODO: canCall
    object IMPORT_DIRECTIVE : CallType<JetExpression?>()

    object PACKAGE_DIRECTIVE : CallType<JetExpression?>()

    object TYPE : CallType<JetExpression?>()

    public open fun canCall(descriptor: DeclarationDescriptor): Boolean = true
}

public sealed class CallTypeAndReceiver<TReceiver : JetElement?, TCallType : CallType<TReceiver>>(
        val callType: TCallType,
        val receiver: TReceiver
) {
    object DEFAULT : CallTypeAndReceiver<Nothing?, CallType.DEFAULT>(CallType.DEFAULT, null)
    class DOT(receiver: JetExpression) : CallTypeAndReceiver<JetExpression, CallType.DOT>(CallType.DOT, receiver)
    class SAFE(receiver: JetExpression) : CallTypeAndReceiver<JetExpression, CallType.SAFE>(CallType.SAFE, receiver)
    class INFIX(receiver: JetExpression) : CallTypeAndReceiver<JetExpression, CallType.INFIX>(CallType.INFIX, receiver)
    class UNARY(receiver: JetExpression) : CallTypeAndReceiver<JetExpression, CallType.UNARY>(CallType.UNARY, receiver)
    class CALLABLE_REFERENCE(receiver: JetTypeReference?) : CallTypeAndReceiver<JetTypeReference?, CallType.CALLABLE_REFERENCE>(CallType.CALLABLE_REFERENCE, receiver)
    class IMPORT_DIRECTIVE(receiver: JetExpression?) : CallTypeAndReceiver<JetExpression?, CallType.IMPORT_DIRECTIVE>(CallType.IMPORT_DIRECTIVE, receiver)
    class PACKAGE_DIRECTIVE(receiver: JetExpression?) : CallTypeAndReceiver<JetExpression?, CallType.PACKAGE_DIRECTIVE>(CallType.PACKAGE_DIRECTIVE, receiver)
    class TYPE(receiver: JetExpression?) : CallTypeAndReceiver<JetExpression?, CallType.TYPE>(CallType.TYPE, receiver)

    companion object {
        public fun detect(expression: JetSimpleNameExpression): CallTypeAndReceiver<*, *> {
            val parent = expression.parent
            if (parent is JetCallableReferenceExpression) {
                return CallTypeAndReceiver.CALLABLE_REFERENCE(parent.typeReference)
            }

            val receiverExpression = expression.getReceiverExpression()

            if (expression.isImportDirectiveExpression()) {
                return CallTypeAndReceiver.IMPORT_DIRECTIVE(receiverExpression)
            }

            if (expression.isPackageDirectiveExpression()) {
                return CallTypeAndReceiver.PACKAGE_DIRECTIVE(receiverExpression)
            }

            if (parent is JetUserType) {
                return CallTypeAndReceiver.TYPE(receiverExpression)
            }

            if (receiverExpression == null) {
                return CallTypeAndReceiver.DEFAULT
            }

            return when (parent) {
                is JetBinaryExpression -> CallTypeAndReceiver.INFIX(receiverExpression)

                is JetCallExpression -> {
                    if ((parent.parent as JetQualifiedExpression).operationSign == JetTokens.SAFE_ACCESS)
                        CallTypeAndReceiver.SAFE(receiverExpression)
                    else
                        CallTypeAndReceiver.DOT(receiverExpression)
                }

                is JetQualifiedExpression -> {
                    if (parent.operationSign == JetTokens.SAFE_ACCESS)
                        CallTypeAndReceiver.SAFE(receiverExpression)
                    else
                        CallTypeAndReceiver.DOT(receiverExpression)
                }

                is JetUnaryExpression -> CallTypeAndReceiver.UNARY(receiverExpression)

                else -> error("Unknown parent for expression with receiver: $parent")
            }
        }
    }
}
