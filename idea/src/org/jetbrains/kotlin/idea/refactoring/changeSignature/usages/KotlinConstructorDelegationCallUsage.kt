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

package org.jetbrains.kotlin.idea.refactoring.changeSignature.usages

import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinChangeInfo
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtSecondaryConstructor

public class KotlinConstructorDelegationCallUsage(
        call: KtConstructorDelegationCall,
        changeInfo: KotlinChangeInfo
) : KotlinUsageInfo<KtConstructorDelegationCall>(call) {
    val delegate = KotlinFunctionCallUsage(call, changeInfo.methodDescriptor.originalPrimaryCallable)

    override fun processUsage(changeInfo: KotlinChangeInfo, element: KtConstructorDelegationCall, allUsages: Array<out UsageInfo>): Boolean {
        val isThisCall = element.isCallToThis()

        var elementToWorkWith = element
        if (changeInfo.getNewParametersCount() > 0 && element.isImplicit()) {
            val constructor = element.getParent() as KtSecondaryConstructor
            elementToWorkWith = constructor.replaceImplicitDelegationCallWithExplicit(isThisCall)
        }

        val result = delegate.processUsage(changeInfo, elementToWorkWith, allUsages)

        if (changeInfo.getNewParametersCount() == 0 && !isThisCall && !elementToWorkWith.isImplicit()) {
            (elementToWorkWith.getParent() as? KtSecondaryConstructor)?.getColon()?.delete()
            elementToWorkWith.replace(KtPsiFactory(element).createConstructorDelegationCall(""))
        }

        return result
    }
}