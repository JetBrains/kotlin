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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.psi.PsiReference
import com.intellij.refactoring.changeSignature.ParameterInfo
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.compareDescriptors
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.core.quoteIfNeeded
import org.jetbrains.kotlin.idea.core.setDefaultValue
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinCallableDefinitionUsage
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.addRemoveModifier.setModifierList
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.types.isError
import java.util.*

class KotlinParameterInfo @JvmOverloads constructor (
        val callableDescriptor: CallableDescriptor,
        val originalIndex: Int = -1,
        private var name: String,
        val originalTypeInfo: KotlinTypeInfo = KotlinTypeInfo(false),
        var defaultValueForParameter: KtExpression? = null,
        var defaultValueForCall: KtExpression? = null,
        var valOrVar: KotlinValVar = KotlinValVar.None,
        val modifierList: KtModifierList? = null
): ParameterInfo {
    var currentTypeInfo: KotlinTypeInfo = originalTypeInfo

    val defaultValueParameterReferences: Map<PsiReference, DeclarationDescriptor>

    init {
        val file = defaultValueForCall?.containingFile as? KtFile
        defaultValueParameterReferences =
                if (defaultValueForCall != null && file != null && (file.isPhysical || file.analysisContext != null)) {
                    val project = file.project
                    val map = LinkedHashMap<PsiReference, DeclarationDescriptor>()

                    defaultValueForCall!!.accept(
                            object : KtTreeVisitorVoid() {
                                private fun selfParameterOrNull(parameter: DeclarationDescriptor?): ValueParameterDescriptor? {
                                    return if (parameter is ValueParameterDescriptor &&
                                               compareDescriptors(project, parameter.containingDeclaration, callableDescriptor)) {
                                        parameter
                                    } else null
                                }

                                private fun selfReceiverOrNull(receiverDescriptor: DeclarationDescriptor?): DeclarationDescriptor? {
                                    if (compareDescriptors(project,
                                                           receiverDescriptor,
                                                           callableDescriptor.extensionReceiverParameter?.containingDeclaration)) {
                                        return receiverDescriptor
                                    }
                                    if (compareDescriptors(project,
                                                           receiverDescriptor,
                                                           callableDescriptor.dispatchReceiverParameter?.containingDeclaration)) {
                                        return receiverDescriptor
                                    }
                                    return null
                                }

                                private fun selfReceiverOrNull(receiver: ImplicitReceiver?): DeclarationDescriptor? {
                                    return selfReceiverOrNull(receiver?.declarationDescriptor)
                                }

                                private fun getRelevantDescriptor(
                                        expression: KtSimpleNameExpression,
                                        ref: KtReference
                                ): DeclarationDescriptor? {
                                    val context = expression.analyze(BodyResolveMode.PARTIAL)

                                    val descriptor = ref.resolveToDescriptors(context).singleOrNull()
                                    if (descriptor is ValueParameterDescriptor) return selfParameterOrNull(descriptor)

                                    if (descriptor is PropertyDescriptor && callableDescriptor is ConstructorDescriptor) {
                                        val parameter = DescriptorToSourceUtils.getSourceFromDescriptor(descriptor) as? KtParameter
                                        return parameter?.let { selfParameterOrNull(context[BindingContext.VALUE_PARAMETER, it]) }
                                    }

                                    val resolvedCall = expression.getResolvedCall(context) ?: return null
                                    (resolvedCall.resultingDescriptor as? ReceiverParameterDescriptor)?.let {
                                        return if (selfReceiverOrNull(it.containingDeclaration) != null) it else null
                                    }

                                    selfReceiverOrNull(resolvedCall.extensionReceiver as? ImplicitReceiver)?.let { return it }
                                    selfReceiverOrNull(resolvedCall.dispatchReceiver as? ImplicitReceiver)?.let { return it }

                                    return null
                                }

                                override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                                    val ref = expression.mainReference
                                    val descriptor = getRelevantDescriptor(expression, ref) ?: return
                                    map[ref] = descriptor
                                }
                            }
                    )

                    map
                }
                else {
                    emptyMap()
                }
    }

    override fun getOldIndex(): Int = originalIndex

    val isNewParameter: Boolean
        get() = originalIndex == -1

    override fun getDefaultValue(): String? = null

    override fun getName(): String = name

    override fun setName(name: String?) {
        this.name = name ?: ""
    }

    override fun getTypeText(): String = currentTypeInfo.render()

    val isTypeChanged: Boolean get() = !currentTypeInfo.isEquivalentTo(originalTypeInfo)

    override fun isUseAnySingleVariable(): Boolean = false

    override fun setUseAnySingleVariable(b: Boolean) {
        throw UnsupportedOperationException()
    }

    fun renderType(parameterIndex: Int, inheritedCallable: KotlinCallableDefinitionUsage<*>): String {
        val defaultRendering = currentTypeInfo.render()
        val typeSubstitutor = inheritedCallable.typeSubstitutor ?: return defaultRendering
        val currentBaseFunction = inheritedCallable.baseFunction.currentCallableDescriptor ?: return defaultRendering
        val parameterType = currentBaseFunction.valueParameters[parameterIndex].type
        if (parameterType.isError) return defaultRendering
        return parameterType.renderTypeWithSubstitution(typeSubstitutor, defaultRendering, true)
    }

    fun getInheritedName(inheritedCallable: KotlinCallableDefinitionUsage<*>): String {
        if (!inheritedCallable.isInherited) return name

        val baseFunction = inheritedCallable.baseFunction
        val baseFunctionDescriptor = baseFunction.originalCallableDescriptor

        val inheritedFunctionDescriptor = inheritedCallable.originalCallableDescriptor
        val inheritedParameterDescriptors = inheritedFunctionDescriptor.valueParameters
        if (originalIndex < 0
            || originalIndex >= baseFunctionDescriptor.valueParameters.size
            || originalIndex >= inheritedParameterDescriptors.size) return name

        val inheritedParamName = inheritedParameterDescriptors[originalIndex].name.asString()
        val oldParamName = baseFunctionDescriptor.valueParameters[originalIndex].name.asString()

        return when {
            oldParamName == inheritedParamName && inheritedFunctionDescriptor !is AnonymousFunctionDescriptor -> name
            else -> inheritedParamName
        }
    }

    fun requiresExplicitType(inheritedCallable: KotlinCallableDefinitionUsage<*>): Boolean {
        val inheritedFunctionDescriptor = inheritedCallable.originalCallableDescriptor
        if (inheritedFunctionDescriptor !is AnonymousFunctionDescriptor) return true

        if (originalIndex < 0) return !inheritedCallable.hasExpectedType

        val inheritedParameterDescriptor = inheritedFunctionDescriptor.valueParameters[originalIndex]
        val parameter = DescriptorToSourceUtils.descriptorToDeclaration(inheritedParameterDescriptor) as? KtParameter ?: return false
        return parameter.typeReference != null
    }

    private fun getOriginalParameter(inheritedCallable: KotlinCallableDefinitionUsage<*>): KtParameter? {
        val currentFunction = inheritedCallable.declaration as? KtFunction ?: return null
        val originalParameterIndex = if (currentFunction.receiverTypeReference == null) originalIndex else originalIndex - 1
        return currentFunction.valueParameters.getOrNull(originalParameterIndex)
    }

    private fun buildNewParameter(inheritedCallable: KotlinCallableDefinitionUsage<*>, parameterIndex: Int): KtParameter {
        val psiFactory = KtPsiFactory(inheritedCallable.project)

        val buffer = StringBuilder()

        if (modifierList != null) {
            buffer.append(modifierList.text).append(' ')
        }

        if (valOrVar != KotlinValVar.None) {
            buffer.append(valOrVar).append(' ')
        }

        buffer.append(getInheritedName(inheritedCallable).quoteIfNeeded())

        if (requiresExplicitType(inheritedCallable)) {
            buffer.append(": ").append(renderType(parameterIndex, inheritedCallable))
        }

        if (!inheritedCallable.isInherited) {
            defaultValueForParameter?.let { buffer.append(" = ").append(it.text) }
        }

        return psiFactory.createParameter(buffer.toString())
    }

    fun getDeclarationSignature(parameterIndex: Int, inheritedCallable: KotlinCallableDefinitionUsage<*>): KtParameter {
        val originalParameter = getOriginalParameter(inheritedCallable)
                                ?: return buildNewParameter(inheritedCallable, parameterIndex)

        val psiFactory = KtPsiFactory(originalParameter)
        val newParameter = originalParameter.copied()

        modifierList?.let { newParameter.setModifierList(it) }

        if (valOrVar != newParameter.valOrVarKeyword.toValVar()) {
            newParameter.setValOrVar(valOrVar)
        }

        val newName = getInheritedName(inheritedCallable)
        if (newParameter.name != newName) {
            newParameter.setName(newName.quoteIfNeeded())
        }

        if (newParameter.typeReference != null || requiresExplicitType(inheritedCallable)) {
            newParameter.typeReference = psiFactory.createType(renderType(parameterIndex, inheritedCallable))
        }

        if (!inheritedCallable.isInherited) {
            defaultValueForParameter?.let { newParameter.setDefaultValue(it) }
        }

        return newParameter
    }
}
