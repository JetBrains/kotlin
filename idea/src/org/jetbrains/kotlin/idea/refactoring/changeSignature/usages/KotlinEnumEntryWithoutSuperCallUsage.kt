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
import org.jetbrains.kotlin.psi.*

public class KotlinEnumEntryWithoutSuperCallUsage(enumEntry: KtEnumEntry) : KotlinUsageInfo<KtEnumEntry>(enumEntry) {
    override fun processUsage(changeInfo: KotlinChangeInfo, element: KtEnumEntry, allUsages: Array<out UsageInfo>): Boolean {
        if (changeInfo.newParameters.size() > 0) {
            val psiFactory = KtPsiFactory(element)

            val delegatorToSuperCall = (element.addAfter(
                    psiFactory.createEnumEntryInitializerList(),
                    element.nameIdentifier
            ) as KtInitializerList).initializers[0] as KtDelegatorToSuperCall

            return KotlinFunctionCallUsage(delegatorToSuperCall, changeInfo.methodDescriptor.originalPrimaryCallable)
                    .processUsage(changeInfo, delegatorToSuperCall, allUsages)
        }

        return true
    }
}
