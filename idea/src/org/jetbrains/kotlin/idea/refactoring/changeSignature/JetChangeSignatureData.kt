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
import org.jetbrains.kotlin.asJava.KotlinLightMethod
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.JetCallableDefinitionUsage
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchOverriders
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetCallableDeclaration
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.psi.JetFunction
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import java.util.Collections
import java.util.HashSet
import kotlin.properties.Delegates

public class JetChangeSignatureData(
        override val baseDescriptor: CallableDescriptor,
        override val baseDeclaration: PsiElement,
        private val descriptorsForSignatureChange: Collection<CallableDescriptor>
) : JetMethodDescriptor {
    private val parameters: List<JetParameterInfo>
    override val receiver: JetParameterInfo?

    init {
        $receiver = createReceiverInfoIfNeeded()

        val valueParameters = when {
            baseDeclaration is JetFunction -> baseDeclaration.getValueParameters()
            baseDeclaration is JetClass -> baseDeclaration.getPrimaryConstructorParameters()
            else -> null
        }
        parameters = baseDescriptor.getValueParameters()
                .mapTo(receiver?.let{ arrayListOf(it) } ?: arrayListOf()) { parameterDescriptor ->
                    val jetParameter = valueParameters?.get(parameterDescriptor.getIndex())
                    JetParameterInfo(
                            callableDescriptor = baseDescriptor,
                            originalIndex = parameterDescriptor.getIndex(),
                            name = parameterDescriptor.getName().asString(),
                            type = parameterDescriptor.getType(),
                            defaultValueForParameter = jetParameter?.getDefaultValue(),
                            valOrVar = jetParameter?.getValOrVarKeyword().toValVar(),
                            modifierList = jetParameter?.getModifierList()
                    )
                }
    }

    private fun createReceiverInfoIfNeeded(): JetParameterInfo? {
        val callable = baseDeclaration as? JetCallableDeclaration ?: return null
        val bodyScope = (callable as? JetFunction)?.getBodyExpression()?.let { it.analyze()[BindingContext.RESOLUTION_SCOPE, it] }
        val paramNames = baseDescriptor.getValueParameters().map { it.getName().asString() }
        val validator = bodyScope?.let { bodyScope ->
            CollectingNameValidator(paramNames) {
                val name = Name.identifier(it)
                bodyScope.getLocalVariable(name) == null && bodyScope.getProperties(name).isEmpty()
            }
        } ?: CollectingNameValidator(paramNames)
        val receiverType = baseDescriptor.getExtensionReceiverParameter()?.getType() ?: return null
        val receiverName = KotlinNameSuggester.suggestNamesByType(receiverType, validator, "receiver").first()
        return JetParameterInfo(callableDescriptor = baseDescriptor, name = receiverName, type = receiverType)
    }

    override val original: JetMethodDescriptor
        get() = this

    override val primaryCallables: Collection<JetCallableDefinitionUsage<PsiElement>> by Delegates.lazy {
        descriptorsForSignatureChange.map {
            val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(baseDeclaration.getProject(), it)
            assert(declaration != null) { "No declaration found for " + baseDescriptor }
            JetCallableDefinitionUsage<PsiElement>(declaration!!, it, null, null)
        }
    }

    override val originalPrimaryCallable: JetCallableDefinitionUsage<PsiElement> by Delegates.lazy {
        primaryCallables.first { it.getDeclaration() == baseDeclaration }
    }

    override val affectedCallables: Collection<UsageInfo> by Delegates.lazy {
        primaryCallables + primaryCallables.flatMapTo(HashSet<UsageInfo>()) { primaryFunction ->
            val primaryDeclaration = primaryFunction.getDeclaration() as? JetCallableDeclaration
            val lightMethods = primaryDeclaration?.toLightMethods() ?: Collections.emptyList()
            lightMethods.flatMap { baseMethod ->
                OverridingMethodsSearch
                        .search(baseMethod)
                        .map { overridingMethod ->
                            if (overridingMethod is KotlinLightMethod) {
                                val overridingDeclaration = overridingMethod.namedUnwrappedElement as JetNamedDeclaration
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
