/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.changeSignature.usages

import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinChangeInfo
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.types.KotlinType

abstract class JavaMethodDeferredKotlinUsage<T : PsiElement>(element: T) : UsageInfo(element) {
    abstract fun resolve(javaMethodChangeInfo: KotlinChangeInfo): JavaMethodKotlinUsageWithDelegate<T>
}

class DeferredJavaMethodOverrideOrSAMUsage(
    val function: KtFunction,
    val functionDescriptor: FunctionDescriptor,
    val samCallType: KotlinType?
) : JavaMethodDeferredKotlinUsage<KtFunction>(function) {
    override fun resolve(javaMethodChangeInfo: KotlinChangeInfo): JavaMethodKotlinUsageWithDelegate<KtFunction> =
        object : JavaMethodKotlinUsageWithDelegate<KtFunction>(function, javaMethodChangeInfo) {
            override val delegateUsage = KotlinCallableDefinitionUsage(
                function,
                functionDescriptor,
                javaMethodChangeInfo.methodDescriptor.originalPrimaryCallable,
                samCallType
            )
        }
}

class DeferredJavaMethodKotlinCallerUsage(
    val declaration: KtNamedDeclaration
) : JavaMethodDeferredKotlinUsage<KtNamedDeclaration>(declaration) {
    override fun resolve(javaMethodChangeInfo: KotlinChangeInfo): JavaMethodKotlinUsageWithDelegate<KtNamedDeclaration> {
        return object : JavaMethodKotlinUsageWithDelegate<KtNamedDeclaration>(declaration, javaMethodChangeInfo) {
            override val delegateUsage = KotlinCallerUsage(declaration)
        }
    }
}
