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

package org.jetbrains.kotlin.idea.refactoring.safeDelete

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiReference
import com.intellij.refactoring.safeDelete.JavaSafeDeleteDelegate
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceSimpleDeleteUsageInfo
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.parameterIndex
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.idea.caches.resolve.analyze

class KotlinJavaSafeDeleteDelegate : JavaSafeDeleteDelegate {
    override fun createUsageInfoForParameter(
            reference: PsiReference, usages: MutableList<UsageInfo>, parameter: PsiParameter, method: PsiMethod
    ) {
        if (reference !is KtReference) return

        val element = reference.element

        val callExpression = element.getNonStrictParentOfType<KtCallExpression>() ?: return

        val calleeExpression = callExpression.calleeExpression
        if (!(calleeExpression is KtReferenceExpression && calleeExpression.isAncestor(element))) return

        val bindingContext = element.analyze()

        val descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, calleeExpression) ?: return

        val originalDeclaration = method.unwrapped
        if (originalDeclaration !is PsiMethod && originalDeclaration !is KtDeclaration) return

        if (originalDeclaration != DescriptorToSourceUtils.descriptorToDeclaration(descriptor)) return

        val args = callExpression.valueArguments

        val namedArguments = args.filter { arg -> arg is KtValueArgument && arg.getArgumentName()?.text == parameter.name }
        if (!namedArguments.isEmpty()) {
            usages.add(SafeDeleteValueArgumentListUsageInfo(parameter, namedArguments.first()))
            return
        }

        val originalParameter = parameter.unwrapped ?: return

        val parameterIndex = originalParameter.parameterIndex()
        if (parameterIndex < 0) return

        val argCount = args.size
        if (parameterIndex < argCount) {
            usages.add(SafeDeleteValueArgumentListUsageInfo(parameter, args[parameterIndex] as KtValueArgument))
        } else {
            val lambdaArgs = callExpression.lambdaArguments
            val lambdaIndex = parameterIndex - argCount
            if (lambdaIndex < lambdaArgs.size) {
                usages.add(SafeDeleteReferenceSimpleDeleteUsageInfo(lambdaArgs[lambdaIndex], parameter, true))
            }
        }
    }
}
