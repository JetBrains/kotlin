/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.psi.psiUtil.isImportDirectiveExpression
import org.jetbrains.kotlin.psi.psiUtil.isPackageDirectiveExpression
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindExclude
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

@Suppress("ClassName")
sealed class CallType<TReceiver : KtElement?>(val descriptorKindFilter: DescriptorKindFilter) {
    object UNKNOWN : CallType<Nothing?>(DescriptorKindFilter.ALL)

    object DEFAULT : CallType<Nothing?>(DescriptorKindFilter.ALL)

    object DOT : CallType<KtExpression>(DescriptorKindFilter.ALL)

    object SAFE : CallType<KtExpression>(DescriptorKindFilter.ALL)

    object SUPER_MEMBERS : CallType<KtSuperExpression>(
        DescriptorKindFilter.CALLABLES exclude DescriptorKindExclude.Extensions exclude AbstractMembersExclude
    )

    object INFIX : CallType<KtExpression>(DescriptorKindFilter.FUNCTIONS exclude NonInfixExclude)

    object OPERATOR : CallType<KtExpression>(DescriptorKindFilter.FUNCTIONS exclude NonOperatorExclude)

    object CALLABLE_REFERENCE : CallType<KtExpression?>(DescriptorKindFilter.CALLABLES exclude CallableReferenceExclude)

    object IMPORT_DIRECTIVE : CallType<KtExpression?>(DescriptorKindFilter.ALL)

    object PACKAGE_DIRECTIVE : CallType<KtExpression?>(DescriptorKindFilter.PACKAGES)

    object TYPE : CallType<KtExpression?>(
        DescriptorKindFilter(DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK)
                exclude DescriptorKindExclude.EnumEntry
    )

    object DELEGATE : CallType<KtExpression?>(DescriptorKindFilter.FUNCTIONS exclude NonOperatorExclude)

    object ANNOTATION : CallType<KtExpression?>(
        DescriptorKindFilter(DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK)
                exclude NonAnnotationClassifierExclude
    )

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
        override fun excludes(descriptor: DeclarationDescriptor) /* currently not supported for locals and synthetic */ =
            descriptor !is CallableMemberDescriptor || descriptor.kind == CallableMemberDescriptor.Kind.SYNTHESIZED

        override val fullyExcludedDescriptorKinds: Int
            get() = 0
    }

    private object NonAnnotationClassifierExclude : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor): Boolean {
            if (descriptor !is ClassifierDescriptor) return false
            return descriptor !is ClassDescriptor || descriptor.kind != ClassKind.ANNOTATION_CLASS
        }

        override val fullyExcludedDescriptorKinds: Int get() = 0
    }

    private object AbstractMembersExclude : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor) =
            descriptor is CallableMemberDescriptor && descriptor.modality == Modality.ABSTRACT

        override val fullyExcludedDescriptorKinds: Int
            get() = 0
    }
}

@Suppress("ClassName")
sealed class CallTypeAndReceiver<TReceiver : KtElement?, out TCallType : CallType<TReceiver>>(
    val callType: TCallType,
    val receiver: TReceiver
) {
    object UNKNOWN : CallTypeAndReceiver<Nothing?, CallType.UNKNOWN>(CallType.UNKNOWN, null)
    object DEFAULT : CallTypeAndReceiver<Nothing?, CallType.DEFAULT>(CallType.DEFAULT, null)
    class DOT(receiver: KtExpression) : CallTypeAndReceiver<KtExpression, CallType.DOT>(CallType.DOT, receiver)
    class SAFE(receiver: KtExpression) : CallTypeAndReceiver<KtExpression, CallType.SAFE>(CallType.SAFE, receiver)
    class SUPER_MEMBERS(receiver: KtSuperExpression) : CallTypeAndReceiver<KtSuperExpression, CallType.SUPER_MEMBERS>(
        CallType.SUPER_MEMBERS, receiver
    )

    class INFIX(receiver: KtExpression) : CallTypeAndReceiver<KtExpression, CallType.INFIX>(CallType.INFIX, receiver)
    class OPERATOR(receiver: KtExpression) : CallTypeAndReceiver<KtExpression, CallType.OPERATOR>(CallType.OPERATOR, receiver)
    class CALLABLE_REFERENCE(receiver: KtExpression?) : CallTypeAndReceiver<KtExpression?, CallType.CALLABLE_REFERENCE>(
        CallType.CALLABLE_REFERENCE, receiver
    )

    class IMPORT_DIRECTIVE(receiver: KtExpression?) : CallTypeAndReceiver<KtExpression?, CallType.IMPORT_DIRECTIVE>(
        CallType.IMPORT_DIRECTIVE, receiver
    )

    class PACKAGE_DIRECTIVE(receiver: KtExpression?) :
        CallTypeAndReceiver<KtExpression?, CallType.PACKAGE_DIRECTIVE>(CallType.PACKAGE_DIRECTIVE, receiver)

    class TYPE(receiver: KtExpression?) : CallTypeAndReceiver<KtExpression?, CallType.TYPE>(CallType.TYPE, receiver)
    class DELEGATE(receiver: KtExpression?) : CallTypeAndReceiver<KtExpression?, CallType.DELEGATE>(CallType.DELEGATE, receiver)
    class ANNOTATION(receiver: KtExpression?) : CallTypeAndReceiver<KtExpression?, CallType.ANNOTATION>(CallType.ANNOTATION, receiver)

    companion object {
        fun detect(expression: KtSimpleNameExpression): CallTypeAndReceiver<*, *> {
            val parent = expression.parent
            if (parent is KtCallableReferenceExpression && expression == parent.callableReference) {
                return CALLABLE_REFERENCE(parent.receiverExpression)
            }

            val receiverExpression = expression.getReceiverExpression()

            if (expression.isImportDirectiveExpression()) {
                return IMPORT_DIRECTIVE(receiverExpression)
            }

            if (expression.isPackageDirectiveExpression()) {
                return PACKAGE_DIRECTIVE(receiverExpression)
            }

            if (parent is KtUserType) {
                val constructorCallee = (parent.parent as? KtTypeReference)?.parent as? KtConstructorCalleeExpression
                if (constructorCallee != null && constructorCallee.parent is KtAnnotationEntry) {
                    return ANNOTATION(receiverExpression)
                }

                return TYPE(receiverExpression)
            }

            when (expression) {
                is KtOperationReferenceExpression -> {
                    if (receiverExpression == null) {
                        return UNKNOWN // incomplete code
                    }
                    return when (parent) {
                        is KtBinaryExpression -> {
                            if (parent.operationToken == KtTokens.IDENTIFIER)
                                INFIX(receiverExpression)
                            else
                                OPERATOR(receiverExpression)
                        }

                        is KtUnaryExpression -> OPERATOR(receiverExpression)

                        else -> error("Unknown parent for JetOperationReferenceExpression: $parent with text '${parent.text}'")
                    }
                }

                is KtNameReferenceExpression -> {
                    if (receiverExpression == null) {
                        return DEFAULT
                    }

                    if (receiverExpression is KtSuperExpression) {
                        return SUPER_MEMBERS(receiverExpression)
                    }

                    return when (parent) {
                        is KtCallExpression -> {
                            if ((parent.parent as KtQualifiedExpression).operationSign == KtTokens.SAFE_ACCESS)
                                SAFE(receiverExpression)
                            else
                                DOT(receiverExpression)
                        }

                        is KtQualifiedExpression -> {
                            if (parent.operationSign == KtTokens.SAFE_ACCESS)
                                SAFE(receiverExpression)
                            else
                                DOT(receiverExpression)
                        }

                        else -> error("Unknown parent for JetNameReferenceExpression with receiver: $parent")
                    }
                }

                else -> return UNKNOWN
            }
        }
    }
}
