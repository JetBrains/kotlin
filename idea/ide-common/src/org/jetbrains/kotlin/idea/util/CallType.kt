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

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.psi.psiUtil.isImportDirectiveExpression
import org.jetbrains.kotlin.psi.psiUtil.isPackageDirectiveExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastManager
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindExclude
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList

public sealed class CallType<TReceiver : JetElement?>(val descriptorKindFilter: DescriptorKindFilter) {
    object UNKNOWN : CallType<Nothing?>(DescriptorKindFilter.ALL)

    object DEFAULT : CallType<Nothing?>(DescriptorKindFilter.ALL)

    object DOT : CallType<JetExpression>(DescriptorKindFilter.ALL)

    object SAFE : CallType<JetExpression>(DescriptorKindFilter.ALL)

    object INFIX : CallType<JetExpression>(DescriptorKindFilter.FUNCTIONS exclude NonInfixExclude)

    object UNARY : CallType<JetExpression>(DescriptorKindFilter.FUNCTIONS exclude NonUnaryExclude)

    object CALLABLE_REFERENCE : CallType<JetTypeReference?>(DescriptorKindFilter.CALLABLES exclude LocalsAndSyntheticExclude/* currently not supported for locals and synthetic */)

    object IMPORT_DIRECTIVE : CallType<JetExpression?>(DescriptorKindFilter.ALL)

    object PACKAGE_DIRECTIVE : CallType<JetExpression?>(DescriptorKindFilter.PACKAGES)

    object TYPE : CallType<JetExpression?>(DescriptorKindFilter(DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK))

    private object NonInfixExclude : DescriptorKindExclude {
        //TODO: check 'infix' modifier
        override fun excludes(descriptor: DeclarationDescriptor) =
                !(descriptor is SimpleFunctionDescriptor && descriptor.valueParameters.size() == 1)

        override val fullyExcludedDescriptorKinds: Int
            get() = 0
    }

    private object NonUnaryExclude : DescriptorKindExclude {
        //TODO: check 'operator' modifier
        override fun excludes(descriptor: DeclarationDescriptor) =
                !(descriptor is SimpleFunctionDescriptor && descriptor.valueParameters.isEmpty())

        override val fullyExcludedDescriptorKinds: Int
            get() = 0
    }

    private object LocalsAndSyntheticExclude : DescriptorKindExclude {
        override fun excludes(descriptor: DeclarationDescriptor)
                = descriptor !is CallableMemberDescriptor || descriptor.kind == CallableMemberDescriptor.Kind.SYNTHESIZED

        override val fullyExcludedDescriptorKinds: Int
            get() = 0
    }

}

public sealed class CallTypeAndReceiver<TReceiver : JetElement?, TCallType : CallType<TReceiver>>(
        val callType: TCallType,
        val receiver: TReceiver
) {
    object UNKNOWN : CallTypeAndReceiver<Nothing?, CallType.UNKNOWN>(CallType.UNKNOWN, null)
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

public fun CallTypeAndReceiver<*, *>.receiverTypes(
        bindingContext: BindingContext,
        position: JetExpression,
        moduleDescriptor: ModuleDescriptor,
        predictableSmartCastsOnly: Boolean
): Collection<JetType>? {
    val receiverExpression: JetExpression?
    when (this) {
        is CallTypeAndReceiver.CALLABLE_REFERENCE -> {
            if (receiver != null) {
                val type = bindingContext[BindingContext.TYPE, receiver]
                return type.singletonOrEmptyList()
            }
            else {
                receiverExpression = null
            }
        }

        is CallTypeAndReceiver.DEFAULT -> receiverExpression = null
        is CallTypeAndReceiver.DOT -> receiverExpression = receiver
        is CallTypeAndReceiver.SAFE -> receiverExpression = receiver
        is CallTypeAndReceiver.INFIX -> receiverExpression = receiver
        is CallTypeAndReceiver.UNARY -> receiverExpression = receiver

        is CallTypeAndReceiver.IMPORT_DIRECTIVE,
        is CallTypeAndReceiver.PACKAGE_DIRECTIVE,
        is CallTypeAndReceiver.TYPE,
        is CallTypeAndReceiver.UNKNOWN ->
            return null

        else -> throw RuntimeException() //TODO: see KT-9394
    }

    val receiverValues = if (receiverExpression != null) {
        val expressionType = bindingContext.getType(receiverExpression)
        expressionType?.let { listOf(ExpressionReceiver(receiverExpression, expressionType)) } ?: return emptyList()
    }
    else {
        val resolutionScope = ReferenceVariantsHelper.resolutionScope(position, bindingContext) ?: return emptyList()
        resolutionScope.getImplicitReceiversWithInstance().map { it.value }
    }

    val dataFlowInfo = ReferenceVariantsHelper.dataFlowInfo(position, bindingContext)

    return receiverValues.flatMap { receiverValue ->
        val dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiverValue, bindingContext, moduleDescriptor)
        if (dataFlowValue.isPredictable || !predictableSmartCastsOnly) { // we don't include smart cast receiver types for "unpredictable" receiver value to mark members grayed
            SmartCastManager().getSmartCastVariantsWithLessSpecificExcluded(receiverValue, bindingContext, moduleDescriptor, dataFlowInfo)
        }
        else {
            listOf(receiverValue.type)
        }
    }
}