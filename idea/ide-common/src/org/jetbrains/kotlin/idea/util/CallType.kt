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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.psi.psiUtil.isImportDirectiveExpression
import org.jetbrains.kotlin.psi.psiUtil.isPackageDirectiveExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastManager
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindExclude
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.KtType
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList

public sealed class CallType<TReceiver : KtElement?>(val descriptorKindFilter: DescriptorKindFilter) {
    object UNKNOWN : CallType<Nothing?>(DescriptorKindFilter.ALL)

    object DEFAULT : CallType<Nothing?>(DescriptorKindFilter.ALL)

    object DOT : CallType<KtExpression>(DescriptorKindFilter.ALL)

    object SAFE : CallType<KtExpression>(DescriptorKindFilter.ALL)

    object INFIX : CallType<KtExpression>(DescriptorKindFilter.FUNCTIONS exclude NonInfixExclude)

    object OPERATOR : CallType<KtExpression>(DescriptorKindFilter.FUNCTIONS exclude NonOperatorExclude)

    object CALLABLE_REFERENCE : CallType<KtTypeReference?>(DescriptorKindFilter.CALLABLES exclude CallableReferenceExclude)

    object IMPORT_DIRECTIVE : CallType<KtExpression?>(DescriptorKindFilter.ALL)

    object PACKAGE_DIRECTIVE : CallType<KtExpression?>(DescriptorKindFilter.PACKAGES)

    object TYPE : CallType<KtExpression?>(DescriptorKindFilter(DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK))

    object ANNOTATION : CallType<KtExpression?>(DescriptorKindFilter(DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK) exclude NonAnnotationClassifierExclude)

    private object NonInfixExclude : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor) =
                !(descriptor is SimpleFunctionDescriptor && descriptor.isInfix)

        override val fullyExcludedDescriptorKinds: Int
            get() = 0
    }

    private object NonOperatorExclude : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor) =
                !(descriptor is SimpleFunctionDescriptor && descriptor.isOperator)

        override val fullyExcludedDescriptorKinds: Int
            get() = 0
    }

    private object CallableReferenceExclude : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor) /* currently not supported for locals, synthetic and genetic */
                = descriptor !is CallableMemberDescriptor || descriptor.kind == CallableMemberDescriptor.Kind.SYNTHESIZED || descriptor.typeParameters.isNotEmpty()

        override val fullyExcludedDescriptorKinds: Int
            get() = 0
    }

    private object NonAnnotationClassifierExclude : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor): Boolean {
            if (descriptor !is ClassifierDescriptor) return false
            return descriptor !is ClassDescriptor || descriptor.getKind() != ClassKind.ANNOTATION_CLASS
        }

        override val fullyExcludedDescriptorKinds: Int get() = 0
    }
}

public sealed class CallTypeAndReceiver<TReceiver : KtElement?, TCallType : CallType<TReceiver>>(
        val callType: TCallType,
        val receiver: TReceiver
) {
    object UNKNOWN : CallTypeAndReceiver<Nothing?, CallType.UNKNOWN>(CallType.UNKNOWN, null)
    object DEFAULT : CallTypeAndReceiver<Nothing?, CallType.DEFAULT>(CallType.DEFAULT, null)
    class DOT(receiver: KtExpression) : CallTypeAndReceiver<KtExpression, CallType.DOT>(CallType.DOT, receiver)
    class SAFE(receiver: KtExpression) : CallTypeAndReceiver<KtExpression, CallType.SAFE>(CallType.SAFE, receiver)
    class INFIX(receiver: KtExpression) : CallTypeAndReceiver<KtExpression, CallType.INFIX>(CallType.INFIX, receiver)
    class OPERATOR(receiver: KtExpression) : CallTypeAndReceiver<KtExpression, CallType.OPERATOR>(CallType.OPERATOR, receiver)
    class CALLABLE_REFERENCE(receiver: KtTypeReference?) : CallTypeAndReceiver<KtTypeReference?, CallType.CALLABLE_REFERENCE>(CallType.CALLABLE_REFERENCE, receiver)
    class IMPORT_DIRECTIVE(receiver: KtExpression?) : CallTypeAndReceiver<KtExpression?, CallType.IMPORT_DIRECTIVE>(CallType.IMPORT_DIRECTIVE, receiver)
    class PACKAGE_DIRECTIVE(receiver: KtExpression?) : CallTypeAndReceiver<KtExpression?, CallType.PACKAGE_DIRECTIVE>(CallType.PACKAGE_DIRECTIVE, receiver)
    class TYPE(receiver: KtExpression?) : CallTypeAndReceiver<KtExpression?, CallType.TYPE>(CallType.TYPE, receiver)
    class ANNOTATION(receiver: KtExpression?) : CallTypeAndReceiver<KtExpression?, CallType.ANNOTATION>(CallType.ANNOTATION, receiver)

    companion object {
        public fun detect(expression: KtSimpleNameExpression): CallTypeAndReceiver<*, *> {
            val parent = expression.parent
            if (parent is KtCallableReferenceExpression) {
                return CallTypeAndReceiver.CALLABLE_REFERENCE(parent.typeReference)
            }

            val receiverExpression = expression.getReceiverExpression()

            if (expression.isImportDirectiveExpression()) {
                return CallTypeAndReceiver.IMPORT_DIRECTIVE(receiverExpression)
            }

            if (expression.isPackageDirectiveExpression()) {
                return CallTypeAndReceiver.PACKAGE_DIRECTIVE(receiverExpression)
            }

            if (parent is KtUserType) {
                val constructorCallee = (parent.parent as? KtTypeReference)?.parent as? KtConstructorCalleeExpression
                if (constructorCallee != null && constructorCallee.parent is KtAnnotationEntry) {
                    return CallTypeAndReceiver.ANNOTATION(receiverExpression)
                }

                return CallTypeAndReceiver.TYPE(receiverExpression)
            }

            when (expression) {
                is KtOperationReferenceExpression -> {
                    if (receiverExpression == null) {
                        return UNKNOWN // incomplete code
                    }
                    return when (parent) {
                        is KtBinaryExpression -> {
                            if (parent.operationToken == KtTokens.IDENTIFIER)
                                CallTypeAndReceiver.INFIX(receiverExpression)
                            else
                                CallTypeAndReceiver.OPERATOR(receiverExpression)
                        }

                        is KtUnaryExpression -> CallTypeAndReceiver.OPERATOR(receiverExpression)

                        else -> error("Unknown parent for JetOperationReferenceExpression: $parent")
                    }
                }

                is KtNameReferenceExpression -> {
                    if (receiverExpression == null) {
                        return CallTypeAndReceiver.DEFAULT
                    }

                    return when (parent) {
                        is KtCallExpression -> {
                            if ((parent.parent as KtQualifiedExpression).operationSign == KtTokens.SAFE_ACCESS)
                                CallTypeAndReceiver.SAFE(receiverExpression)
                            else
                                CallTypeAndReceiver.DOT(receiverExpression)
                        }

                        is KtQualifiedExpression -> {
                            if (parent.operationSign == KtTokens.SAFE_ACCESS)
                                CallTypeAndReceiver.SAFE(receiverExpression)
                            else
                                CallTypeAndReceiver.DOT(receiverExpression)
                        }

                        else -> error("Unknown parent for JetNameReferenceExpression with receiver: $parent")
                    }
                }

                else -> return UNKNOWN
            }
        }
    }
}

public fun CallTypeAndReceiver<*, *>.receiverTypes(
        bindingContext: BindingContext,
        position: KtExpression,
        moduleDescriptor: ModuleDescriptor,
        predictableSmartCastsOnly: Boolean
): Collection<KtType>? {
    val receiverExpression: KtExpression?
    when (this) {
        is CallTypeAndReceiver.CALLABLE_REFERENCE -> {
            return receiver?.let { bindingContext[BindingContext.TYPE, it] }.singletonOrEmptyList()
        }

        is CallTypeAndReceiver.DEFAULT -> receiverExpression = null
        is CallTypeAndReceiver.DOT -> receiverExpression = receiver
        is CallTypeAndReceiver.SAFE -> receiverExpression = receiver
        is CallTypeAndReceiver.INFIX -> receiverExpression = receiver
        is CallTypeAndReceiver.OPERATOR -> receiverExpression = receiver

        is CallTypeAndReceiver.IMPORT_DIRECTIVE,
        is CallTypeAndReceiver.PACKAGE_DIRECTIVE,
        is CallTypeAndReceiver.TYPE,
        is CallTypeAndReceiver.ANNOTATION,
        is CallTypeAndReceiver.UNKNOWN ->
            return null

        else -> throw RuntimeException() //TODO: see KT-9394
    }

    val receiverValues = if (receiverExpression != null) {
        val expressionType = bindingContext.getType(receiverExpression)
        expressionType?.let { listOf(ExpressionReceiver(receiverExpression, expressionType)) } ?: return emptyList()
    }
    else {
        val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, position] ?: return emptyList()
        resolutionScope.getImplicitReceiversWithInstance().map { it.value }
    }

    val dataFlowInfo = bindingContext.getDataFlowInfo(position)

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