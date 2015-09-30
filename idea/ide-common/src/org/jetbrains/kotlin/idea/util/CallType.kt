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

public enum class CallType {
    NORMAL,
    SAFE,

    INFIX {
        override fun canCall(descriptor: DeclarationDescriptor)
                = descriptor is SimpleFunctionDescriptor && descriptor.getValueParameters().size() == 1
    },

    UNARY {
        override fun canCall(descriptor: DeclarationDescriptor)
                = descriptor is SimpleFunctionDescriptor && descriptor.getValueParameters().size() == 0
    },

    CALLABLE_REFERENCE {
        // currently callable references to locals and parameters are not supported
        override fun canCall(descriptor: DeclarationDescriptor)
                = descriptor is FunctionDescriptor || descriptor is PropertyDescriptor
    },

    //TODO: canCall
    IMPORT_DIRECTIVE,

    PACKAGE_DIRECTIVE

    ;

    public open fun canCall(descriptor: DeclarationDescriptor): Boolean = true
}

public data class CallTypeAndReceiver(
        val callType: CallType,
        val receiver: JetElement?
) {
    companion object {
        public fun detect(expression: JetSimpleNameExpression): CallTypeAndReceiver {
            val parent = expression.parent
            if (parent is JetCallableReferenceExpression) {
                return CallTypeAndReceiver(CallType.CALLABLE_REFERENCE, parent.typeReference)
            }

            val receiverExpression = expression.getReceiverExpression()

            if (expression.isImportDirectiveExpression()) {
                return CallTypeAndReceiver(CallType.IMPORT_DIRECTIVE, receiverExpression)
            }

            if (expression.isPackageDirectiveExpression()) {
                return CallTypeAndReceiver(CallType.PACKAGE_DIRECTIVE, receiverExpression)
            }

            if (receiverExpression == null) {
                return CallTypeAndReceiver(CallType.NORMAL, null)
            }

            val callType = when (parent) {
                is JetBinaryExpression -> CallType.INFIX

                is JetCallExpression -> {
                    if ((parent.getParent() as JetQualifiedExpression).getOperationSign() == JetTokens.SAFE_ACCESS)
                        CallType.SAFE
                    else
                        CallType.NORMAL
                }

                is JetQualifiedExpression -> {
                    if (parent.getOperationSign() == JetTokens.SAFE_ACCESS)
                        CallType.SAFE
                    else
                        CallType.NORMAL
                }

                is JetUnaryExpression -> CallType.UNARY

                is JetUserType -> CallType.NORMAL

                else -> error("Unknown parent for expression with receiver: $parent")
            }
            return CallTypeAndReceiver(callType, receiverExpression)
        }
    }
}
