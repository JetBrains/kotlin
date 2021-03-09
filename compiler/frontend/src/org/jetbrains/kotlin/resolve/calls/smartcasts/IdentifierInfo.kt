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

package org.jetbrains.kotlin.resolve.calls.smartcasts

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorEquivalenceForOverrides
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.isSafeCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue.Kind.OTHER
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue.Kind.STABLE_VALUE
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner

interface IdentifierInfo {

    val kind: DataFlowValue.Kind get() = OTHER

    val canBeBound get() = false

    object NO : IdentifierInfo {
        override fun toString() = "NO_IDENTIFIER_INFO"
    }

    object NULL : IdentifierInfo {
        override fun toString() = "NULL"
    }

    object ERROR : IdentifierInfo {
        override fun toString() = "ERROR"
    }

    class Variable(
        val variable: VariableDescriptor,
        override val kind: DataFlowValue.Kind,
        val bound: DataFlowValue?
    ) : IdentifierInfo {
        override val canBeBound
            get() = kind == STABLE_VALUE

        override fun equals(other: Any?) =
            other is Variable &&
                    DescriptorEquivalenceForOverrides.areCallableDescriptorsEquivalent(
                        variable, other.variable, allowCopiesFromTheSameDeclaration = true, kotlinTypeRefiner = KotlinTypeRefiner.Default
                    )

        override fun hashCode() = variable.name.hashCode() * 31 + variable.containingDeclaration.original.hashCode()

        override fun toString() = variable.toString()
    }

    data class Receiver(val value: ReceiverValue) : IdentifierInfo {
        override val kind = STABLE_VALUE

        override fun toString() = value.toString()
    }

    data class PackageOrClass(val descriptor: DeclarationDescriptor) : IdentifierInfo {
        override val kind = STABLE_VALUE

        override fun toString() = descriptor.toString()
    }

    data class EnumEntry(val descriptor: ClassDescriptor) : IdentifierInfo {
        override val kind: DataFlowValue.Kind = STABLE_VALUE
    }

    class Qualified(
        val receiverInfo: IdentifierInfo,
        val selectorInfo: IdentifierInfo,
        val safe: Boolean,
        val receiverType: KotlinType?
    ) : IdentifierInfo {

        override val kind: DataFlowValue.Kind get() = if (receiverInfo.kind == STABLE_VALUE) selectorInfo.kind else OTHER

        override val canBeBound
            get() = receiverInfo.canBeBound

        override fun equals(other: Any?) = other is Qualified && receiverInfo == other.receiverInfo && selectorInfo == other.selectorInfo

        override fun hashCode() = 31 * receiverInfo.hashCode() + selectorInfo.hashCode()

        override fun toString() = "$receiverInfo${if (safe) "?." else "."}$selectorInfo"
    }

    data class SafeCast(val subjectInfo: IdentifierInfo, val subjectType: KotlinType?, val targetType: KotlinType?) : IdentifierInfo {
        override val kind get() = OTHER

        override val canBeBound get() = subjectInfo.canBeBound

        override fun toString() = "$subjectInfo as? ${targetType ?: "???"}"
    }

    class Expression(val expression: KtExpression, stableComplex: Boolean = false) : IdentifierInfo {
        override val kind = if (stableComplex) DataFlowValue.Kind.STABLE_COMPLEX_EXPRESSION else OTHER

        override fun equals(other: Any?) = other is Expression && expression == other.expression

        override fun hashCode() = expression.hashCode()

        override fun toString() = expression.text ?: "(empty expression)"
    }

    // For only ++ and -- postfix operations
    data class PostfixIdentifierInfo(val argumentInfo: IdentifierInfo, val op: KtToken) : IdentifierInfo {
        override val kind: DataFlowValue.Kind get() = argumentInfo.kind

        override fun toString() = "$argumentInfo($op)"
    }
}

internal fun getIdForStableIdentifier(
    expression: KtExpression?,
    bindingContext: BindingContext,
    containingDeclarationOrModule: DeclarationDescriptor,
    languageVersionSettings: LanguageVersionSettings
): IdentifierInfo {
    if (expression != null) {
        val deparenthesized = KtPsiUtil.deparenthesize(expression)
        if (expression !== deparenthesized) {
            return getIdForStableIdentifier(deparenthesized, bindingContext, containingDeclarationOrModule, languageVersionSettings)
        }
    }
    return when (expression) {
        is KtQualifiedExpression -> {
            val receiverExpression = expression.receiverExpression
            val selectorExpression = expression.selectorExpression
            val receiverInfo =
                getIdForStableIdentifier(receiverExpression, bindingContext, containingDeclarationOrModule, languageVersionSettings)
            val selectorInfo =
                getIdForStableIdentifier(selectorExpression, bindingContext, containingDeclarationOrModule, languageVersionSettings)

            qualified(
                receiverInfo, bindingContext.getType(receiverExpression),
                selectorInfo, expression.operationSign === KtTokens.SAFE_ACCESS
            )
        }

        is KtBinaryExpressionWithTypeRHS -> {
            val subjectExpression = expression.left
            val targetTypeReference = expression.right
            val operationToken = expression.operationReference.getReferencedNameElementType()
            if (operationToken == KtTokens.IS_KEYWORD || operationToken == KtTokens.AS_KEYWORD) {
                IdentifierInfo.NO
            } else {
                IdentifierInfo.SafeCast(
                    getIdForStableIdentifier(subjectExpression, bindingContext, containingDeclarationOrModule, languageVersionSettings),
                    bindingContext.getType(subjectExpression),
                    bindingContext[BindingContext.TYPE, targetTypeReference]
                )
            }
        }

        is KtSimpleNameExpression ->
            getIdForSimpleNameExpression(expression, bindingContext, containingDeclarationOrModule, languageVersionSettings)

        is KtThisExpression -> {
            val declarationDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expression.instanceReference)
            getIdForThisReceiver(declarationDescriptor, bindingContext, expression.getLabelName())
        }

        is KtPostfixExpression -> {
            val operationType = expression.operationReference.getReferencedNameElementType()
            if (operationType === KtTokens.PLUSPLUS || operationType === KtTokens.MINUSMINUS)
                postfix(
                    getIdForStableIdentifier(
                        expression.baseExpression,
                        bindingContext,
                        containingDeclarationOrModule,
                        languageVersionSettings
                    ),
                    operationType
                )
            else
                IdentifierInfo.NO
        }

        else -> IdentifierInfo.NO
    }
}

private fun getIdForSimpleNameExpression(
    simpleNameExpression: KtSimpleNameExpression,
    bindingContext: BindingContext,
    containingDeclarationOrModule: DeclarationDescriptor,
    languageVersionSettings: LanguageVersionSettings
): IdentifierInfo {
    val declarationDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, simpleNameExpression)
    return when (declarationDescriptor) {
        is VariableDescriptor -> {
            val resolvedCall = simpleNameExpression.getResolvedCall(bindingContext)

            // todo uncomment assert
            // KT-4113
            // for now it fails for resolving 'invoke' convention, return it after 'invoke' algorithm changes
            // assert resolvedCall != null : "Cannot create right identifier info if the resolved call is not known yet for
            val usageModuleDescriptor = DescriptorUtils.getContainingModuleOrNull(containingDeclarationOrModule)
            val selectorInfo = IdentifierInfo.Variable(
                declarationDescriptor,
                declarationDescriptor.variableKind(usageModuleDescriptor, bindingContext, simpleNameExpression, languageVersionSettings),
                bindingContext[BindingContext.BOUND_INITIALIZER_VALUE, declarationDescriptor]
            )

            val implicitReceiver = resolvedCall?.dispatchReceiver
            if (implicitReceiver == null) {
                selectorInfo
            } else {
                val receiverInfo = getIdForImplicitReceiver(implicitReceiver, simpleNameExpression, bindingContext)

                if (receiverInfo == null) {
                    selectorInfo
                } else {
                    qualified(
                        receiverInfo, implicitReceiver.type,
                        selectorInfo, resolvedCall.call.isSafeCall()
                    )
                }
            }
        }

        is ClassDescriptor -> {
            if (declarationDescriptor.kind == ClassKind.ENUM_ENTRY && languageVersionSettings.supportsFeature(LanguageFeature.SoundSmartcastForEnumEntries))
                IdentifierInfo.EnumEntry(declarationDescriptor)
            else
                IdentifierInfo.PackageOrClass(declarationDescriptor)
        }

        is PackageViewDescriptor -> IdentifierInfo.PackageOrClass(declarationDescriptor)

        else -> IdentifierInfo.NO
    }
}

private fun getIdForImplicitReceiver(receiverValue: ReceiverValue?, expression: KtExpression?, bindingContext: BindingContext) =
    when (receiverValue) {
        is ImplicitReceiver -> getIdForThisReceiver(receiverValue.declarationDescriptor, bindingContext)

        is TransientReceiver ->
            throw AssertionError("Transient receiver is implicit for an explicit expression: $expression. Receiver: $receiverValue")

        else -> null
    }

private fun getIdForThisReceiver(
    descriptorOfThisReceiver: DeclarationDescriptor?,
    bindingContext: BindingContext,
    labelName: String? = null
) =
    when (descriptorOfThisReceiver) {
        is CallableDescriptor -> {
            val receiverParameter = findReceiverByLabelOrGetDefault(
                descriptorOfThisReceiver,
                descriptorOfThisReceiver.extensionReceiverParameter ?: descriptorOfThisReceiver.contextReceiverParameters.firstOrNull(),
                bindingContext,
                labelName
            )
            IdentifierInfo.Receiver(receiverParameter.value)
        }

        is ClassDescriptor -> {
            val receiverParameter = findReceiverByLabelOrGetDefault(
                descriptorOfThisReceiver,
                descriptorOfThisReceiver.thisAsReceiverParameter,
                bindingContext,
                labelName
            )
            IdentifierInfo.Receiver(receiverParameter.value)
        }

        else -> IdentifierInfo.NO
    }

private fun findReceiverByLabelOrGetDefault(
    descriptorOfThisReceiver: DeclarationDescriptor,
    default: ReceiverParameterDescriptor?,
    bindingContext: BindingContext,
    labelName: String? = null
): ReceiverParameterDescriptor {
    val receiverToLabelMap = bindingContext.get(
        BindingContext.DESCRIPTOR_TO_NAMED_RECEIVERS,
        if (descriptorOfThisReceiver is PropertyAccessorDescriptor) descriptorOfThisReceiver.correspondingProperty else descriptorOfThisReceiver
    )
    return receiverToLabelMap?.entries?.find {
        it.value == labelName
    }?.key ?: default ?: error("'This' refers to the callable member without a receiver parameter: $descriptorOfThisReceiver")
}

private fun postfix(argumentInfo: IdentifierInfo, op: KtToken): IdentifierInfo =
    if (argumentInfo == IdentifierInfo.NO) IdentifierInfo.NO else IdentifierInfo.PostfixIdentifierInfo(argumentInfo, op)

private fun qualified(receiverInfo: IdentifierInfo, receiverType: KotlinType?, selectorInfo: IdentifierInfo, safe: Boolean) =
    when (receiverInfo) {
        IdentifierInfo.NO -> IdentifierInfo.NO
        is IdentifierInfo.PackageOrClass -> selectorInfo
        else -> IdentifierInfo.Qualified(receiverInfo, selectorInfo, safe, receiverType)
    }
