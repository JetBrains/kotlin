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

package org.jetbrains.kotlin.idea.util

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.psi.psiUtil.isImportDirectiveExpression
import org.jetbrains.kotlin.psi.psiUtil.isPackageDirectiveExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoBefore
import org.jetbrains.kotlin.resolve.calls.DslMarkerUtils
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastManager
import org.jetbrains.kotlin.resolve.descriptorUtil.classValueType
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindExclude
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassQualifier
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.TypeAliasQualifier
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS
import org.jetbrains.kotlin.util.supertypesWithAny
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.*

sealed class CallType<TReceiver : KtElement?>(val descriptorKindFilter: DescriptorKindFilter) {
    object UNKNOWN : CallType<Nothing?>(DescriptorKindFilter.ALL)

    object DEFAULT : CallType<Nothing?>(DescriptorKindFilter.ALL)

    object DOT : CallType<KtExpression>(DescriptorKindFilter.ALL)

    object SAFE : CallType<KtExpression>(DescriptorKindFilter.ALL)

    object SUPER_MEMBERS :
        CallType<KtSuperExpression>(DescriptorKindFilter.CALLABLES exclude DescriptorKindExclude.Extensions exclude AbstractMembersExclude)

    object INFIX : CallType<KtExpression>(DescriptorKindFilter.FUNCTIONS exclude NonInfixExclude)

    object OPERATOR : CallType<KtExpression>(DescriptorKindFilter.FUNCTIONS exclude NonOperatorExclude)

    object CALLABLE_REFERENCE : CallType<KtExpression?>(DescriptorKindFilter.CALLABLES exclude CallableReferenceExclude)

    object IMPORT_DIRECTIVE : CallType<KtExpression?>(DescriptorKindFilter.ALL)

    object PACKAGE_DIRECTIVE : CallType<KtExpression?>(DescriptorKindFilter.PACKAGES)

    object TYPE :
        CallType<KtExpression?>(DescriptorKindFilter(DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK) exclude DescriptorKindExclude.EnumEntry)

    object DELEGATE : CallType<KtExpression?>(DescriptorKindFilter.FUNCTIONS exclude NonOperatorExclude)

    object ANNOTATION :
        CallType<KtExpression?>(DescriptorKindFilter(DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK) exclude NonAnnotationClassifierExclude)

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

sealed class CallTypeAndReceiver<TReceiver : KtElement?, out TCallType : CallType<TReceiver>>(
    val callType: TCallType,
    val receiver: TReceiver
) {
    object UNKNOWN : CallTypeAndReceiver<Nothing?, CallType.UNKNOWN>(CallType.UNKNOWN, null)
    object DEFAULT : CallTypeAndReceiver<Nothing?, CallType.DEFAULT>(CallType.DEFAULT, null)
    class DOT(receiver: KtExpression) : CallTypeAndReceiver<KtExpression, CallType.DOT>(CallType.DOT, receiver)
    class SAFE(receiver: KtExpression) : CallTypeAndReceiver<KtExpression, CallType.SAFE>(CallType.SAFE, receiver)
    class SUPER_MEMBERS(receiver: KtSuperExpression) :
        CallTypeAndReceiver<KtSuperExpression, CallType.SUPER_MEMBERS>(CallType.SUPER_MEMBERS, receiver)

    class INFIX(receiver: KtExpression) : CallTypeAndReceiver<KtExpression, CallType.INFIX>(CallType.INFIX, receiver)
    class OPERATOR(receiver: KtExpression) : CallTypeAndReceiver<KtExpression, CallType.OPERATOR>(CallType.OPERATOR, receiver)
    class CALLABLE_REFERENCE(receiver: KtExpression?) :
        CallTypeAndReceiver<KtExpression?, CallType.CALLABLE_REFERENCE>(CallType.CALLABLE_REFERENCE, receiver)

    class IMPORT_DIRECTIVE(receiver: KtExpression?) :
        CallTypeAndReceiver<KtExpression?, CallType.IMPORT_DIRECTIVE>(CallType.IMPORT_DIRECTIVE, receiver)

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

data class ReceiverType(
    val type: KotlinType,
    val receiverIndex: Int,
    val implicitValue: ReceiverValue? = null
) {
    val implicit: Boolean get() = implicitValue != null

    fun extractDslMarkers() =
        implicitValue?.let(DslMarkerUtils::extractDslMarkerFqNames)?.all()
            ?: DslMarkerUtils.extractDslMarkerFqNames(type)
}

fun CallTypeAndReceiver<*, *>.receiverTypes(
    bindingContext: BindingContext,
    contextElement: PsiElement,
    moduleDescriptor: ModuleDescriptor,
    resolutionFacade: ResolutionFacade,
    stableSmartCastsOnly: Boolean
): Collection<KotlinType>? {
    return receiverTypesWithIndex(bindingContext, contextElement, moduleDescriptor, resolutionFacade, stableSmartCastsOnly)?.map { it.type }
}

fun CallTypeAndReceiver<*, *>.receiverTypesWithIndex(
    bindingContext: BindingContext,
    contextElement: PsiElement,
    moduleDescriptor: ModuleDescriptor,
    resolutionFacade: ResolutionFacade,
    stableSmartCastsOnly: Boolean,
    withImplicitReceiversWhenExplicitPresent: Boolean = false
): Collection<ReceiverType>? {
    val languageVersionSettings = resolutionFacade.frontendService<LanguageVersionSettings>()

    val receiverExpression: KtExpression?
    when (this) {
        is CallTypeAndReceiver.CALLABLE_REFERENCE -> {
            if (receiver != null) {
                return when (val lhs = bindingContext[BindingContext.DOUBLE_COLON_LHS, receiver] ?: return emptyList()) {
                    is DoubleColonLHS.Type -> listOf(ReceiverType(lhs.type, 0))

                    is DoubleColonLHS.Expression -> {
                        val receiverValue = ExpressionReceiver.create(receiver, lhs.type, bindingContext)
                        receiverValueTypes(
                            receiverValue, lhs.dataFlowInfo, bindingContext,
                            moduleDescriptor, stableSmartCastsOnly, languageVersionSettings,
                            resolutionFacade.frontendService()
                        ).map { ReceiverType(it, 0) }
                    }
                }
            } else {
                return emptyList()
            }
        }

        is CallTypeAndReceiver.DEFAULT -> receiverExpression = null

        is CallTypeAndReceiver.DOT -> receiverExpression = receiver
        is CallTypeAndReceiver.SAFE -> receiverExpression = receiver
        is CallTypeAndReceiver.INFIX -> receiverExpression = receiver
        is CallTypeAndReceiver.OPERATOR -> receiverExpression = receiver
        is CallTypeAndReceiver.DELEGATE -> receiverExpression = receiver

        is CallTypeAndReceiver.SUPER_MEMBERS -> {
            val qualifier = receiver.superTypeQualifier
            return if (qualifier != null) {
                listOfNotNull(bindingContext.getType(receiver)).map { ReceiverType(it, 0) }
            } else {
                val resolutionScope = contextElement.getResolutionScope(bindingContext, resolutionFacade)
                val classDescriptor =
                    resolutionScope.ownerDescriptor.parentsWithSelf.firstIsInstanceOrNull<ClassDescriptor>() ?: return emptyList()
                classDescriptor.typeConstructor.supertypesWithAny().map { ReceiverType(it, 0) }
            }
        }

        is CallTypeAndReceiver.IMPORT_DIRECTIVE,
        is CallTypeAndReceiver.PACKAGE_DIRECTIVE,
        is CallTypeAndReceiver.TYPE,
        is CallTypeAndReceiver.ANNOTATION,
        is CallTypeAndReceiver.UNKNOWN ->
            return null
    }

    val resolutionScope = contextElement.getResolutionScope(bindingContext, resolutionFacade)

    val expressionReceiver = receiverExpression?.let {
        val receiverType =
            bindingContext.getType(receiverExpression) ?: (bindingContext.get(
                BindingContext.QUALIFIER,
                receiverExpression
            ) as? ClassQualifier)?.descriptor?.classValueType ?: (bindingContext.get(
                BindingContext.QUALIFIER,
                receiverExpression
            ) as? TypeAliasQualifier)?.classDescriptor?.classValueType ?: return emptyList()
        ExpressionReceiver.create(receiverExpression, receiverType, bindingContext)
    }

    val implicitReceiverValues = resolutionScope.getImplicitReceiversWithInstance(
        excludeShadowedByDslMarkers = languageVersionSettings.supportsFeature(LanguageFeature.DslMarkersSupport)
    ).map { it.value }

    val dataFlowInfo = bindingContext.getDataFlowInfoBefore(contextElement)

    val result = ArrayList<ReceiverType>()

    var receiverIndex = 0

    fun addReceiverType(receiverValue: ReceiverValue, implicit: Boolean) {
        val types = receiverValueTypes(
            receiverValue, dataFlowInfo, bindingContext, moduleDescriptor, stableSmartCastsOnly, languageVersionSettings,
            resolutionFacade.frontendService()
        )

        types.mapTo(result) { type -> ReceiverType(type, receiverIndex, receiverValue.takeIf { implicit }) }

        receiverIndex++
    }
    if (withImplicitReceiversWhenExplicitPresent || expressionReceiver == null) {
        implicitReceiverValues.forEach { addReceiverType(it, true) }
    }
    if (expressionReceiver != null) {
        addReceiverType(expressionReceiver, false)
    }
    return result
}

private fun receiverValueTypes(
    receiverValue: ReceiverValue,
    dataFlowInfo: DataFlowInfo,
    bindingContext: BindingContext,
    moduleDescriptor: ModuleDescriptor,
    stableSmartCastsOnly: Boolean,
    languageVersionSettings: LanguageVersionSettings,
    dataFlowValueFactory: DataFlowValueFactory
): List<KotlinType> {
    val dataFlowValue = dataFlowValueFactory.createDataFlowValue(receiverValue, bindingContext, moduleDescriptor)
    return if (dataFlowValue.isStable || !stableSmartCastsOnly) { // we don't include smart cast receiver types for "unstable" receiver value to mark members grayed
        SmartCastManager().getSmartCastVariantsWithLessSpecificExcluded(
            receiverValue,
            bindingContext,
            moduleDescriptor,
            dataFlowInfo,
            languageVersionSettings,
            dataFlowValueFactory
        )
    } else {
        listOf(receiverValue.type)
    }
}
