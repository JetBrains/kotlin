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
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetChangeInfo
import org.jetbrains.kotlin.psi.JetConstructorDelegationCall
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetSecondaryConstructor

public class JetConstructorDelegationCallUsage(
        call: JetConstructorDelegationCall,
        changeInfo: JetChangeInfo
) : JetUsageInfo<JetConstructorDelegationCall>(call) {
    val delegate = JetFunctionCallUsage(call, changeInfo.methodDescriptor.originalPrimaryCallable)

    override fun processUsage(changeInfo: JetChangeInfo, element: JetConstructorDelegationCall, allUsages: Array<out UsageInfo>): Boolean {
        val isThisCall = element.isCallToThis()

        var elementToWorkWith = element
        if (changeInfo.getNewParametersCount() > 0 && element.isImplicit()) {
            val constructor = element.getParent() as JetSecondaryConstructor
            elementToWorkWith = constructor.replaceImplicitDelegationCallWithExplicit(isThisCall)
        }

        val result = delegate.processUsage(changeInfo, elementToWorkWith, allUsages)

        if (changeInfo.getNewParametersCount() == 0 && !isThisCall && !elementToWorkWith.isImplicit()) {
            (elementToWorkWith.getParent() as? JetSecondaryConstructor)?.getColon()?.delete()
            elementToWorkWith.replace(JetPsiFactory(element).createConstructorDelegationCall(""))
        }

        return result
    }
}