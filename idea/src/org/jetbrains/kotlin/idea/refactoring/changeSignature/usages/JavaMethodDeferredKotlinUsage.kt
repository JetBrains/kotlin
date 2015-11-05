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
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinChangeInfo
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.types.KotlinType

public abstract class JavaMethodDeferredKotlinUsage<T : PsiElement>(element: T) : UsageInfo(element) {
    abstract fun resolve(javaMethodChangeInfo: KotlinChangeInfo): JavaMethodKotlinUsageWithDelegate<T>
}

public class DeferredJavaMethodOverrideOrSAMUsage(
        val function: KtFunction,
        val functionDescriptor: FunctionDescriptor,
        val samCallType: KotlinType?
) : JavaMethodDeferredKotlinUsage<KtFunction>(function) {
    override fun resolve(javaMethodChangeInfo: KotlinChangeInfo): JavaMethodKotlinUsageWithDelegate<KtFunction> {
        return object : JavaMethodKotlinUsageWithDelegate<KtFunction>(function, javaMethodChangeInfo) {
            override val delegateUsage = KotlinCallableDefinitionUsage(function, functionDescriptor, javaMethodChangeInfo.methodDescriptor.originalPrimaryCallable, samCallType)
        }
    }
}

public class DeferredJavaMethodKotlinCallerUsage(
        val declaration: KtNamedDeclaration
) : JavaMethodDeferredKotlinUsage<KtNamedDeclaration>(declaration) {
    override fun resolve(javaMethodChangeInfo: KotlinChangeInfo): JavaMethodKotlinUsageWithDelegate<KtNamedDeclaration> {
        return object : JavaMethodKotlinUsageWithDelegate<KtNamedDeclaration>(declaration, javaMethodChangeInfo) {
            override val delegateUsage = KotlinCallerUsage(declaration)
        }
    }
}

public class JavaConstructorDeferredUsageInDelegationCall(
        val delegationCall: KtConstructorDelegationCall
) : JavaMethodDeferredKotlinUsage<KtConstructorDelegationCall>(delegationCall) {
    override fun resolve(javaMethodChangeInfo: KotlinChangeInfo): JavaMethodKotlinUsageWithDelegate<KtConstructorDelegationCall> {
        return object : JavaMethodKotlinUsageWithDelegate<KtConstructorDelegationCall>(delegationCall, javaMethodChangeInfo) {
            override val delegateUsage = KotlinConstructorDelegationCallUsage(delegationCall, javaMethodChangeInfo)
        }
    }
}