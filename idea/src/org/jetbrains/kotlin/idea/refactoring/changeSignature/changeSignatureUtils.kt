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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.refactoring.changeSignature.CallerUsageInfo
import com.intellij.refactoring.changeSignature.OverriderUsageInfo
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.DeferredJavaMethodKotlinCallerUsage
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.JavaMethodKotlinUsageWithDelegate
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinCallableDefinitionUsage
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinCallerUsage
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.scopes.utils.findVariable
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.substitutions.getCallableSubstitutor
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

fun KtNamedDeclaration.getDeclarationBody(): KtElement? {
    return when {
        this is KtClassOrObject -> getSuperTypeList()
        this is KtPrimaryConstructor -> getContainingClassOrObject().getSuperTypeList()
        this is KtSecondaryConstructor -> getDelegationCall()
        this is KtNamedFunction -> bodyExpression
        else -> null
    }
}

fun PsiElement.isCaller(allUsages: Array<out UsageInfo>): Boolean {
    val primaryConstructor = (this as? KtClass)?.getPrimaryConstructor()
    val elementsToSearch = if (primaryConstructor != null) listOf(primaryConstructor, this) else listOf(this)
    return allUsages
            .asSequence()
            .filter {
                val usage = (it as? JavaMethodKotlinUsageWithDelegate<*>)?.delegateUsage ?: it
                usage is KotlinCallerUsage
                || usage is DeferredJavaMethodKotlinCallerUsage
                || usage is CallerUsageInfo
                || (usage is OverriderUsageInfo && !usage.isOriginalOverrider)
            }
            .any { it.element in elementsToSearch }
}

fun KtElement.isInsideOfCallerBody(allUsages: Array<out UsageInfo>): Boolean {
    val container = parentsWithSelf.firstOrNull {
        it is KtNamedFunction || it is KtConstructor<*> || it is KtClassOrObject
    } as? KtNamedDeclaration ?: return false
    val body = container.getDeclarationBody() ?: return false
    return body.textRange.contains(textRange) && container.isCaller(allUsages)
}

fun getCallableSubstitutor(
        baseFunction: KotlinCallableDefinitionUsage<*>,
        derivedCallable: KotlinCallableDefinitionUsage<*>
): TypeSubstitutor? {
    val currentBaseFunction = baseFunction.currentCallableDescriptor ?: return null
    val currentDerivedFunction = derivedCallable.currentCallableDescriptor ?: return null
    return getCallableSubstitutor(currentBaseFunction, currentDerivedFunction)
}

fun KotlinType.renderTypeWithSubstitution(substitutor: TypeSubstitutor?, defaultText: String, inArgumentPosition: Boolean): String {
    val newType = substitutor?.substitute(this, Variance.INVARIANT) ?: return defaultText
    val renderer = if (inArgumentPosition) IdeDescriptorRenderers.SOURCE_CODE_NOT_NULL_TYPE_APPROXIMATION else IdeDescriptorRenderers.SOURCE_CODE
    return renderer.renderType(newType)
}

// This method is used to create full copies of functions (including copies of all types)
// It's needed to prevent accesses to PSI (e.g. using LazyJavaClassifierType properties) when Change signature invalidates it
// See KotlinChangeSignatureTest.testSAMChangeMethodReturnType
fun DeclarationDescriptor.createDeepCopy() = (this as? JavaMethodDescriptor)?.substitute(TypeSubstitutor.create(ForceTypeCopySubstitution)) ?: this

private object ForceTypeCopySubstitution : TypeSubstitution() {
    override fun get(key: KotlinType) =
            with(key) {
                if (isError) return@with asTypeProjection()
                KotlinTypeFactory.simpleType(annotations, constructor, arguments, isMarkedNullable, memberScope).asTypeProjection()
            }

    override fun isEmpty() = false
}

fun suggestReceiverNames(project: Project, descriptor: CallableDescriptor): List<String> {
    val callable = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor) as? KtCallableDeclaration ?: return emptyList()
    val bodyScope = (callable as? KtFunction)?.bodyExpression?.let { it.getResolutionScope(it.analyze(), it.getResolutionFacade()) }
    val paramNames = descriptor.valueParameters.map { it.name.asString() }
    val validator = bodyScope?.let { bodyScope ->
        CollectingNameValidator(paramNames) {
            bodyScope.findVariable(Name.identifier(it), NoLookupLocation.FROM_IDE) == null
        }
    } ?: CollectingNameValidator(paramNames)
    val receiverType = descriptor.extensionReceiverParameter?.type ?: return emptyList()
    return KotlinNameSuggester.suggestNamesByType(receiverType, validator, "receiver")
}