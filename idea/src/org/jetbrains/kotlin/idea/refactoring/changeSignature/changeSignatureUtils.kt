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

import com.intellij.psi.PsiElement
import com.intellij.refactoring.changeSignature.CallerUsageInfo
import com.intellij.refactoring.changeSignature.OverriderUsageInfo
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.DeferredJavaMethodKotlinCallerUsage
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.JavaMethodKotlinUsageWithDelegate
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinCallableDefinitionUsage
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinCallerUsage
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.substitutions.getCallableSubstitutor

fun KtNamedDeclaration.getDeclarationBody(): KtElement? {
    return when {
        this is KtClassOrObject -> getDelegationSpecifierList()
        this is KtPrimaryConstructor -> getContainingClassOrObject().getDelegationSpecifierList()
        this is KtSecondaryConstructor -> getDelegationCall()
        this is KtNamedFunction -> getBodyExpression()
        else -> null
    }
}

public fun PsiElement.isCaller(allUsages: Array<out UsageInfo>): Boolean {
    val elementToSearch = (this as? KtClass)?.getPrimaryConstructor() ?: this
    return allUsages
            .asSequence()
            .filter {
                val usage = (it as? JavaMethodKotlinUsageWithDelegate<*>)?.delegateUsage ?: it
                usage is KotlinCallerUsage
                || usage is DeferredJavaMethodKotlinCallerUsage
                || usage is CallerUsageInfo
                || (usage is OverriderUsageInfo && !usage.isOriginalOverrider())
            }
            .any { it.getElement() == elementToSearch }
}

public fun KtElement.isInsideOfCallerBody(allUsages: Array<out UsageInfo>): Boolean {
    val container = parentsWithSelf.firstOrNull {
        it is KtNamedFunction || it is KtConstructor<*> || it is KtClassOrObject
    } as? KtNamedDeclaration ?: return false
    val body = container.getDeclarationBody() ?: return false
    return body.getTextRange().contains(getTextRange()) && container.isCaller(allUsages)
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
    val renderer = if (inArgumentPosition) IdeDescriptorRenderers.SOURCE_CODE_FOR_TYPE_ARGUMENTS else IdeDescriptorRenderers.SOURCE_CODE
    return renderer.renderType(newType)
}