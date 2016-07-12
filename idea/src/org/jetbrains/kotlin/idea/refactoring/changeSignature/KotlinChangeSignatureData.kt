/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.refactoring.changeSignature.MethodDescriptor
import com.intellij.refactoring.changeSignature.OverriderUsageInfo
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.KtLightMethod
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinCallableDefinitionUsage
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.utils.findVariable
import java.util.*

class KotlinChangeSignatureData(
        override val baseDescriptor: CallableDescriptor,
        override val baseDeclaration: PsiElement,
        private val descriptorsForSignatureChange: Collection<CallableDescriptor>
) : KotlinMethodDescriptor {
    private val parameters: List<KotlinParameterInfo>
    override val receiver: KotlinParameterInfo?

    init {
        receiver = createReceiverInfoIfNeeded()

        val valueParameters = when (baseDeclaration) {
            is KtFunction -> baseDeclaration.valueParameters
            is KtClass -> baseDeclaration.getPrimaryConstructorParameters()
            else -> null
        }
        parameters = baseDescriptor.valueParameters
                .mapTo(receiver?.let{ arrayListOf(it) } ?: arrayListOf()) { parameterDescriptor ->
                    val jetParameter = valueParameters?.get(parameterDescriptor.index)
                    val parameterType = parameterDescriptor.type
                    val parameterTypeText = jetParameter?.typeReference?.text
                                            ?: IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(parameterType)
                    KotlinParameterInfo(
                            callableDescriptor = baseDescriptor,
                            originalIndex = parameterDescriptor.index,
                            name = parameterDescriptor.name.asString(),
                            originalTypeInfo = KotlinTypeInfo(false, parameterType, parameterTypeText),
                            defaultValueForParameter = jetParameter?.defaultValue,
                            valOrVar = jetParameter?.valOrVarKeyword.toValVar(),
                            modifierList = jetParameter?.modifierList
                    )
                }
    }

    private fun createReceiverInfoIfNeeded(): KotlinParameterInfo? {
        val callable = baseDeclaration as? KtCallableDeclaration ?: return null
        val bodyScope = (callable as? KtFunction)?.bodyExpression?.let { it.getResolutionScope(it.analyze(), it.getResolutionFacade()) }
        val paramNames = baseDescriptor.valueParameters.map { it.name.asString() }
        val validator = bodyScope?.let { bodyScope ->
            CollectingNameValidator(paramNames) {
                bodyScope.findVariable(Name.identifier(it), NoLookupLocation.FROM_IDE) == null
            }
        } ?: CollectingNameValidator(paramNames)
        val receiverType = baseDescriptor.extensionReceiverParameter?.type ?: return null
        val receiverName = KotlinNameSuggester.suggestNamesByType(receiverType, validator, "receiver").first()
        val receiverTypeText = (baseDeclaration as? KtCallableDeclaration)?.receiverTypeReference?.text
                               ?: IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(receiverType)
        return KotlinParameterInfo(callableDescriptor = baseDescriptor,
                                   name = receiverName,
                                   originalTypeInfo = KotlinTypeInfo(false, receiverType, receiverTypeText))
    }

    override val original: KotlinMethodDescriptor
        get() = this

    override val primaryCallables: Collection<KotlinCallableDefinitionUsage<PsiElement>> by lazy {
        descriptorsForSignatureChange.map {
            val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(baseDeclaration.project, it)
            assert(declaration != null) { "No declaration found for " + baseDescriptor }
            KotlinCallableDefinitionUsage(declaration!!, it, null, null)
        }
    }

    override val originalPrimaryCallable: KotlinCallableDefinitionUsage<PsiElement> by lazy {
        primaryCallables.first { it.declaration == baseDeclaration }
    }

    override val affectedCallables: Collection<UsageInfo> by lazy {
        primaryCallables + primaryCallables.flatMapTo(HashSet<UsageInfo>()) { primaryFunction ->
            val primaryDeclaration = primaryFunction.declaration as? KtCallableDeclaration
            val lightMethods = primaryDeclaration?.toLightMethods() ?: Collections.emptyList()
            lightMethods.flatMap { baseMethod ->
                OverridingMethodsSearch
                        .search(baseMethod)
                        .mapNotNullTo(HashSet<UsageInfo>()) { overridingMethod ->
                            if (overridingMethod is KtLightMethod) {
                                val overridingDeclaration = overridingMethod.namedUnwrappedElement as KtNamedDeclaration
                                val overridingDescriptor = overridingDeclaration.resolveToDescriptor() as CallableDescriptor
                                KotlinCallableDefinitionUsage<PsiElement>(overridingDeclaration, overridingDescriptor, primaryFunction, null)
                            }
                            else OverriderUsageInfo(overridingMethod, baseMethod, true, true, true)
                        }
            }
        }
    }

    override fun getParameters(): List<KotlinParameterInfo> {
        return parameters
    }

    override fun getName(): String {
        if (baseDescriptor is ConstructorDescriptor) {
            return baseDescriptor.containingDeclaration.name.asString()
        }
        else if (baseDescriptor is AnonymousFunctionDescriptor) {
            return ""
        }
        else {
            return baseDescriptor.name.asString()
        }
    }

    override fun getParametersCount(): Int {
        return baseDescriptor.valueParameters.size
    }

    override fun getVisibility(): Visibility {
        return baseDescriptor.visibility
    }

    override fun getMethod(): PsiElement {
        return baseDeclaration
    }

    override fun canChangeVisibility(): Boolean {
        if (DescriptorUtils.isLocal(baseDescriptor)) return false
        val parent = baseDescriptor.containingDeclaration
        return !(baseDescriptor is AnonymousFunctionDescriptor || parent is ClassDescriptor && parent.kind == ClassKind.INTERFACE)
    }

    override fun canChangeParameters(): Boolean {
        return true
    }

    override fun canChangeName(): Boolean {
        return !(baseDescriptor is ConstructorDescriptor || baseDescriptor is AnonymousFunctionDescriptor)
    }

    override fun canChangeReturnType(): MethodDescriptor.ReadWriteOption {
        return if (baseDescriptor is ConstructorDescriptor) MethodDescriptor.ReadWriteOption.None else MethodDescriptor.ReadWriteOption.ReadWrite
    }
}
