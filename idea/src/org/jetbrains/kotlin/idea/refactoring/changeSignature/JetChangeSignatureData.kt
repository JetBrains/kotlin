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
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.JetCallableDefinitionUsage
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.utils.findVariable
import java.util.*

public class JetChangeSignatureData(
        override val baseDescriptor: CallableDescriptor,
        override val baseDeclaration: PsiElement,
        private val descriptorsForSignatureChange: Collection<CallableDescriptor>
) : JetMethodDescriptor {
    private val parameters: List<JetParameterInfo>
    override val receiver: JetParameterInfo?

    init {
        receiver = createReceiverInfoIfNeeded()

        val valueParameters = when {
            baseDeclaration is KtFunction -> baseDeclaration.getValueParameters()
            baseDeclaration is KtClass -> baseDeclaration.getPrimaryConstructorParameters()
            else -> null
        }
        parameters = baseDescriptor.getValueParameters()
                .mapTo(receiver?.let{ arrayListOf(it) } ?: arrayListOf()) { parameterDescriptor ->
                    val jetParameter = valueParameters?.get(parameterDescriptor.index)
                    JetParameterInfo(
                            callableDescriptor = baseDescriptor,
                            originalIndex = parameterDescriptor.index,
                            name = parameterDescriptor.getName().asString(),
                            type = parameterDescriptor.getType(),
                            defaultValueForParameter = jetParameter?.getDefaultValue(),
                            valOrVar = jetParameter?.getValOrVarKeyword().toValVar(),
                            modifierList = jetParameter?.getModifierList()
                    )
                }
    }

    private fun createReceiverInfoIfNeeded(): JetParameterInfo? {
        val callable = baseDeclaration as? KtCallableDeclaration ?: return null
        val bodyScope = (callable as? KtFunction)?.bodyExpression?.let { it.getResolutionScope(it.analyze(), it.getResolutionFacade()) }
        val paramNames = baseDescriptor.valueParameters.map { it.name.asString() }
        val validator = bodyScope?.let { bodyScope ->
            CollectingNameValidator(paramNames) {
                bodyScope.findVariable(Name.identifier(it), NoLookupLocation.FROM_IDE) == null
            }
        } ?: CollectingNameValidator(paramNames)
        val receiverType = baseDescriptor.getExtensionReceiverParameter()?.getType() ?: return null
        val receiverName = KotlinNameSuggester.suggestNamesByType(receiverType, validator, "receiver").first()
        return JetParameterInfo(callableDescriptor = baseDescriptor, name = receiverName, type = receiverType)
    }

    override val original: JetMethodDescriptor
        get() = this

    override val primaryCallables: Collection<JetCallableDefinitionUsage<PsiElement>> by lazy {
        descriptorsForSignatureChange.map {
            val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(baseDeclaration.getProject(), it)
            assert(declaration != null) { "No declaration found for " + baseDescriptor }
            JetCallableDefinitionUsage<PsiElement>(declaration!!, it, null, null)
        }
    }

    override val originalPrimaryCallable: JetCallableDefinitionUsage<PsiElement> by lazy {
        primaryCallables.first { it.declaration == baseDeclaration }
    }

    override val affectedCallables: Collection<UsageInfo> by lazy {
        primaryCallables + primaryCallables.flatMapTo(HashSet<UsageInfo>()) { primaryFunction ->
            val primaryDeclaration = primaryFunction.declaration as? KtCallableDeclaration
            val lightMethods = primaryDeclaration?.toLightMethods() ?: Collections.emptyList()
            lightMethods.flatMap { baseMethod ->
                OverridingMethodsSearch
                        .search(baseMethod)
                        .map { overridingMethod ->
                            if (overridingMethod is KtLightMethod) {
                                val overridingDeclaration = overridingMethod.namedUnwrappedElement as KtNamedDeclaration
                                val overridingDescriptor = overridingDeclaration.resolveToDescriptor() as CallableDescriptor
                                JetCallableDefinitionUsage<PsiElement>(overridingDeclaration, overridingDescriptor, primaryFunction, null)
                            }
                            else OverriderUsageInfo(overridingMethod, baseMethod, true, true, true)
                        }.filterNotNullTo(HashSet<UsageInfo>())
            }
        }
    }

    override fun getParameters(): List<JetParameterInfo> {
        return parameters
    }

    override fun getName(): String {
        if (baseDescriptor is ConstructorDescriptor) {
            return baseDescriptor.getContainingDeclaration().getName().asString()
        }
        else if (baseDescriptor is AnonymousFunctionDescriptor) {
            return ""
        }
        else {
            return baseDescriptor.getName().asString()
        }
    }

    override fun getParametersCount(): Int {
        return baseDescriptor.getValueParameters().size()
    }

    override fun getVisibility(): Visibility {
        return baseDescriptor.getVisibility()
    }

    override fun getMethod(): PsiElement {
        return baseDeclaration
    }

    override fun canChangeVisibility(): Boolean {
        if (DescriptorUtils.isLocal(baseDescriptor)) return false;
        val parent = baseDescriptor.getContainingDeclaration()
        return !(baseDescriptor is AnonymousFunctionDescriptor || parent is ClassDescriptor && parent.getKind() == ClassKind.INTERFACE)
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
