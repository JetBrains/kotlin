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

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceParameter

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.introduceParameter.IntroduceParameterData
import com.intellij.refactoring.introduceParameter.IntroduceParameterMethodUsagesProcessor
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.resolve.getJavaMethodDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.refactoring.dropOverrideKeywordIfNecessary
import org.jetbrains.kotlin.idea.refactoring.j2k
import org.jetbrains.kotlin.idea.refactoring.changeSignature.*
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinCallableDefinitionUsage
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinConstructorDelegationCallUsage
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinFunctionCallUsage
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinUsageInfo
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchOverriders
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import java.util.*

class KotlinIntroduceParameterMethodUsageProcessor : IntroduceParameterMethodUsagesProcessor {
    override fun isMethodUsage(usage: UsageInfo): Boolean = (usage.element as? KtElement)?.let {
        it.getParentOfTypeAndBranch<KtCallElement>(true) { calleeExpression } != null
    } ?: false

    override fun findConflicts(data: IntroduceParameterData, usages: Array<UsageInfo>, conflicts: MultiMap<PsiElement, String>) {

    }

    private fun createChangeInfo(data: IntroduceParameterData, method: PsiElement): KotlinChangeInfo? {
        val psiMethodDescriptor = when (method) {
            is KtFunction -> method.unsafeResolveToDescriptor() as? FunctionDescriptor
            is PsiMethod -> method.getJavaMethodDescriptor()
            else -> null
        } ?: return null
        val changeSignatureData = KotlinChangeSignatureData(psiMethodDescriptor, method, Collections.singletonList(psiMethodDescriptor))
        val changeInfo = KotlinChangeInfo(methodDescriptor = changeSignatureData, context = method)

        data.parametersToRemove.toNativeArray().sortedDescending().forEach { changeInfo.removeParameter(it) }

        // Temporarily assume that the new parameter is of Any type. Actual type is substituted during the signature update phase
        val defaultValueForCall = (data.parameterInitializer.expression as? PsiExpression)?.j2k()
        changeInfo.addParameter(KotlinParameterInfo(callableDescriptor = psiMethodDescriptor,
                                                    name = data.parameterName,
                                                    originalTypeInfo = KotlinTypeInfo(false, psiMethodDescriptor.builtIns.anyType),
                                                    defaultValueForCall = defaultValueForCall))
        return changeInfo
    }

    override fun processChangeMethodSignature(data: IntroduceParameterData, usage: UsageInfo, usages: Array<out UsageInfo>): Boolean {
        val element = usage.element as? KtFunction ?: return true

        val changeInfo = createChangeInfo(data, element) ?: return true
        // Java method is already updated at this point
        val addedParameterType = data.methodToReplaceIn.getJavaMethodDescriptor()!!.valueParameters.last().type
        changeInfo.newParameters.last().currentTypeInfo = KotlinTypeInfo(false, addedParameterType)

        val scope = element.useScope.let {
            if (it is GlobalSearchScope) GlobalSearchScope.getScopeRestrictedByFileTypes(it, KotlinFileType.INSTANCE) else it
        }
        val kotlinFunctions = HierarchySearchRequest(element, scope)
                .searchOverriders()
                .map { it.unwrapped }
                .filterIsInstance<KtFunction>()
        return (kotlinFunctions + element).all {
            KotlinCallableDefinitionUsage(it, changeInfo.originalBaseFunctionDescriptor, null, null, false).processUsage(changeInfo, it, usages)
        }.apply {
            dropOverrideKeywordIfNecessary(element)
        }
    }

    override fun processChangeMethodUsage(data: IntroduceParameterData, usage: UsageInfo, usages: Array<out UsageInfo>): Boolean {
        val psiMethod = data.methodToReplaceIn
        val changeInfo = createChangeInfo(data, psiMethod) ?: return true
        val refElement = usage.element as? KtReferenceExpression ?: return true
        val callElement = refElement.getParentOfTypeAndBranch<KtCallElement>(true) { calleeExpression } ?: return true
        val delegateUsage = if (callElement is KtConstructorDelegationCall) {
            @Suppress("UNCHECKED_CAST")
            (KotlinConstructorDelegationCallUsage(callElement, changeInfo) as KotlinUsageInfo<KtCallElement>)
        }
        else {
            KotlinFunctionCallUsage(callElement, changeInfo.methodDescriptor.originalPrimaryCallable)
        }
        return delegateUsage.processUsage(changeInfo, callElement, usages)
    }

    override fun processAddSuperCall(data: IntroduceParameterData, usage: UsageInfo, usages: Array<out UsageInfo>): Boolean = true

    override fun processAddDefaultConstructor(data: IntroduceParameterData, usage: UsageInfo, usages: Array<out UsageInfo>): Boolean = true
}
