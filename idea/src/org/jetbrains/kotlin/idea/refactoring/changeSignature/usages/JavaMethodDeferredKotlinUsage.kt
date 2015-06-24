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

import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetChangeInfo
import org.jetbrains.kotlin.psi.JetConstructorDelegationCall
import org.jetbrains.kotlin.psi.JetFunction
import org.jetbrains.kotlin.types.JetType

public abstract class JavaMethodDeferredKotlinUsage<T: PsiElement>(element: T): UsageInfo(element) {
        abstract fun resolve(javaMethodChangeInfo: JetChangeInfo): JavaMethodKotlinUsageWithDelegate<T>
}

public class DeferredJavaMethodOverrideOrSAMUsage(
        val function: JetFunction,
        val functionDescriptor: FunctionDescriptor,
        val samCallType: JetType?
): JavaMethodDeferredKotlinUsage<JetFunction>(function) {
        override fun resolve(javaMethodChangeInfo: JetChangeInfo): JavaMethodKotlinUsageWithDelegate<JetFunction> {
                return object : JavaMethodKotlinUsageWithDelegate<JetFunction>(function, javaMethodChangeInfo) {
                        override val delegateUsage = JetCallableDefinitionUsage(
                                function,
                                functionDescriptor,
                                javaMethodChangeInfo.methodDescriptor.originalPrimaryCallable,
                                samCallType
                        )
                }
        }
}

public class JavaConstructorDeferredUsageInDelegationCall(
        val delegationCall: JetConstructorDelegationCall
): JavaMethodDeferredKotlinUsage<JetConstructorDelegationCall>(delegationCall) {
        override fun resolve(javaMethodChangeInfo: JetChangeInfo): JavaMethodKotlinUsageWithDelegate<JetConstructorDelegationCall> {
                return object : JavaMethodKotlinUsageWithDelegate<JetConstructorDelegationCall>(delegationCall, javaMethodChangeInfo) {
                        override val delegateUsage = JetConstructorDelegationCallUsage(delegationCall, javaMethodChangeInfo)
                }
        }
}