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

package org.jetbrains.kotlin.idea.liveTemplates.macro

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.ExpectedInfo
import org.jetbrains.kotlin.idea.core.ExpectedInfos
import org.jetbrains.kotlin.idea.core.SmartCastCalculator
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.idea.util.FuzzyType
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext

class SuitableVariableMacro : BaseKotlinVariableMacro() {
    private companion object {
        val EXPECTED_INFOS_KEY = Key<Collection<ExpectedInfo>>("EXPECTED_INFOS_KEY")
        val SMART_CAST_CALCULATOR_KEY = Key<SmartCastCalculator>("SMART_CAST_CALCULATOR_KEY")
    }

    override fun getName() = "kotlinVariable"
    override fun getPresentableName() = "kotlinVariable()"

    override fun initUserData(userData: UserDataHolder, contextElement: KtElement, bindingContext: BindingContext) {
        val resolutionFacade = contextElement.getResolutionFacade()
        if (contextElement is KtNameReferenceExpression) {
            val callTypeAndReceiver = CallTypeAndReceiver.detect(contextElement)
            if (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT) {
                val expectedInfos = ExpectedInfos(bindingContext, resolutionFacade).calculate(contextElement)
                if (expectedInfos.isNotEmpty()) {
                    userData.putUserData(EXPECTED_INFOS_KEY, expectedInfos)

                    val scope = contextElement.getResolutionScope(bindingContext, resolutionFacade)
                    val smartCastCalculator = SmartCastCalculator(bindingContext, scope.ownerDescriptor, contextElement, null, resolutionFacade)
                    userData.putUserData(SMART_CAST_CALCULATOR_KEY, smartCastCalculator)
                }
            }
        }
    }

    override fun isSuitable(variableDescriptor: VariableDescriptor, project: Project, userData: UserDataHolder): Boolean {
        val expectedInfos = userData.getUserData(EXPECTED_INFOS_KEY) ?: return true
        val smartCastCalculator = userData.getUserData(SMART_CAST_CALCULATOR_KEY)!!
        val types = smartCastCalculator.types(variableDescriptor)
        return expectedInfos.any { expectedInfo -> types.any { expectedInfo.filter.matchingSubstitutor(FuzzyType(it, emptyList())) != null } }
    }
}
