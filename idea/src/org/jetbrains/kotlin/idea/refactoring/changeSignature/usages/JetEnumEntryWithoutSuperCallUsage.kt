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
import org.jetbrains.kotlin.psi.JetEnumEntry
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetChangeInfo
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.psi.JetDelegatorToSuperCall
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

public class JetEnumEntryWithoutSuperCallUsage(enumEntry: JetEnumEntry) : JetUsageInfo<JetEnumEntry>(enumEntry) {
    override fun processUsage(changeInfo: JetChangeInfo, element: JetEnumEntry, allUsages: Array<out UsageInfo>): Boolean {
        if (changeInfo.getNewParameters().size() > 0) {
            val psiFactory = JetPsiFactory(element)

            val enumClass = element.getStrictParentOfType<JetClass>()!!
            val delegatorToSuperCall = element.addAfter(
                    psiFactory.createDelegatorToSuperCall("${enumClass.getName()}()"),
                    element.getNameAsDeclaration()
            ) as JetDelegatorToSuperCall
            element.addBefore(psiFactory.createColon(), delegatorToSuperCall)

            return JetFunctionCallUsage(delegatorToSuperCall, changeInfo.methodDescriptor.originalPrimaryCallable)
                    .processUsage(changeInfo, delegatorToSuperCall, allUsages)
        }

        return true
    }
}
