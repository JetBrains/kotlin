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

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.introduceParameter.IntroduceParameterData
import com.intellij.refactoring.introduceParameter.IntroduceParameterMethodUsagesProcessor
import com.intellij.usageView.UsageInfo
import com.intellij.util.ArrayUtil
import com.intellij.util.containers.MultiMap
import gnu.trove.TIntArrayList
import org.jetbrains.kotlin.asJava.KotlinLightMethod
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.idea.caches.resolve.getJavaMethodDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.refactoring.j2k
import org.jetbrains.kotlin.idea.j2k.IdeaResolverForConverter
import org.jetbrains.kotlin.idea.j2k.J2kPostProcessor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetChangeInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetChangeSignatureData
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetParameterInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.originalBaseFunctionDescriptor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.*
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.j2k.IdeaReferenceSearcher
import org.jetbrains.kotlin.j2k.JavaToKotlinConverter
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.types.JetType
import java.util.Arrays
import java.util.Collections
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchOverriders

public class KotlinIntroduceParameterMethodUsageProcessor : IntroduceParameterMethodUsagesProcessor {
    override fun isMethodUsage(usage: UsageInfo): Boolean = (usage.getElement() as? JetElement)?.let {
        it.getParentOfTypeAndBranch<JetCallElement>(true) { getCalleeExpression() } != null
    } ?: false

    override fun findConflicts(data: IntroduceParameterData, usages: Array<UsageInfo>, conflicts: MultiMap<PsiElement, String>) {

    }

    private fun createChangeInfo(data: IntroduceParameterData, method: PsiElement): JetChangeInfo? {
        val psiMethodDescriptor = when (method) {
            is JetFunction -> method.resolveToDescriptor() as? FunctionDescriptor
            is PsiMethod -> method.getJavaMethodDescriptor()
            else -> null
        } ?: return null
        val changeSignatureData = JetChangeSignatureData(psiMethodDescriptor, method, Collections.singletonList(psiMethodDescriptor))
        val changeInfo = JetChangeInfo(methodDescriptor = changeSignatureData, context = method)

        data.getParametersToRemove().toNativeArray().toList().sortDescending().forEach { changeInfo.removeParameter(it) }

        // Temporarily assume that the new parameter is of Any type. Actual type is substituted during the signature update phase
        val defaultValueForCall = (data.getParameterInitializer().getExpression()!! as? PsiExpression)?.let { it.j2k() }
        changeInfo.addParameter(JetParameterInfo(callableDescriptor = psiMethodDescriptor,
                                                 name = data.getParameterName(),
                                                 type = KotlinBuiltIns.getInstance().getAnyType(),
                                                 defaultValueForCall = defaultValueForCall))
        return changeInfo
    }

    override fun processChangeMethodSignature(data: IntroduceParameterData, usage: UsageInfo, usages: Array<out UsageInfo>): Boolean {
        val element = usage.getElement() as? JetFunction ?: return true

        val changeInfo = createChangeInfo(data, element) ?: return true
        // Java method is already updated at this point
        val addedParameterType = data.getMethodToReplaceIn().getJavaMethodDescriptor().getValueParameters().last().getType()
        changeInfo.getNewParameters().last().currentTypeText = IdeDescriptorRenderers.SOURCE_CODE.renderType(addedParameterType)

        val scope = element.getUseScope().let {
            if (it is GlobalSearchScope) GlobalSearchScope.getScopeRestrictedByFileTypes(it, JetFileType.INSTANCE) else it
        }
        val kotlinFunctions = HierarchySearchRequest(element, scope)
                .searchOverriders()
                .map { it.unwrapped }
                .filterIsInstance<JetFunction>()
        return (kotlinFunctions + element).all {
            JetCallableDefinitionUsage(it, changeInfo.originalBaseFunctionDescriptor, null, null).processUsage(changeInfo, it)
        }
    }

    override fun processChangeMethodUsage(data: IntroduceParameterData, usage: UsageInfo, usages: Array<out UsageInfo>): Boolean {
        val psiMethod = data.getMethodToReplaceIn()
        val changeInfo = createChangeInfo(data, psiMethod) ?: return true
        val refElement = usage.getElement() as? JetReferenceExpression ?: return true
        val callElement = refElement.getParentOfTypeAndBranch<JetCallElement>(true) { getCalleeExpression() } ?: return true
        val delegateUsage = if (callElement is JetConstructorDelegationCall) {
            @suppress("CAST_NEVER_SUCCEEDS")
            (JetConstructorDelegationCallUsage(callElement, changeInfo) as JetUsageInfo<JetCallElement>)
        }
        else {
            JetFunctionCallUsage(callElement, changeInfo.methodDescriptor.originalPrimaryCallable)
        }
        return delegateUsage.processUsage(changeInfo, callElement)
    }

    override fun processAddSuperCall(data: IntroduceParameterData, usage: UsageInfo, usages: Array<out UsageInfo>): Boolean = true

    override fun processAddDefaultConstructor(data: IntroduceParameterData, usage: UsageInfo, usages: Array<out UsageInfo>): Boolean = true
}
