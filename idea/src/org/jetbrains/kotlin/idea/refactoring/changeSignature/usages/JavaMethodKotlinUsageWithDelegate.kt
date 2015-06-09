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

import org.jetbrains.kotlin.psi.JetCallElement
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetChangeInfo
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.psi.JetFunction
import org.jetbrains.kotlin.descriptors.FunctionDescriptor

public abstract class JavaMethodKotlinUsageWithDelegate<T: PsiElement>(
        val psiElement: T,
        val javaMethodChangeInfo: JetChangeInfo): UsageInfo(psiElement) {
    protected abstract val delegateUsage: JetUsageInfo<T>

    fun processUsage(): Boolean = delegateUsage.processUsage(javaMethodChangeInfo, psiElement)
}

public class JavaMethodKotlinCallUsage(
        callElement: JetCallElement,
        javaMethodChangeInfo: JetChangeInfo): JavaMethodKotlinUsageWithDelegate<JetCallElement>(callElement, javaMethodChangeInfo) {
    override protected val delegateUsage = JetFunctionCallUsage(psiElement, javaMethodChangeInfo.methodDescriptor.originalPrimaryFunction)
}

public class JavaMethodKotlinDerivedDefinitionUsage(
        function: JetFunction,
        functionDescriptor: FunctionDescriptor,
        javaMethodChangeInfo: JetChangeInfo): JavaMethodKotlinUsageWithDelegate<JetFunction>(function, javaMethodChangeInfo) {
    @suppress("CAST_NEVER_SUCCEEDS")
    override protected val delegateUsage = JetFunctionDefinitionUsage(
            psiElement,
            functionDescriptor,
            javaMethodChangeInfo.methodDescriptor.originalPrimaryFunction,
            null
    )
}
