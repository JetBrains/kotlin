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
import org.jetbrains.kotlin.idea.references.JetReference
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.parameterIndex
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.idea.caches.resolve.analyze

public class KotlinJavaSafeDeleteDelegate : JavaSafeDeleteDelegate {
    override fun createUsageInfoForParameter(
            reference: PsiReference, usages: MutableList<UsageInfo>, parameter: PsiParameter, method: PsiMethod
    ) {
        if (reference !is JetReference) return

        val element = reference.getElement() as JetElement

        val callExpression = element.getNonStrictParentOfType<JetCallExpression>()
        if (callExpression == null) return

        val calleeExpression = callExpression.getCalleeExpression()
        if (!(calleeExpression is JetReferenceExpression && calleeExpression.isAncestor(element))) return

        val bindingContext = element.analyze()

        val descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, calleeExpression)
        if (descriptor == null) return

        val originalDeclaration = method.unwrapped
        if (originalDeclaration !is PsiMethod && originalDeclaration !is JetDeclaration) return

        if (originalDeclaration != DescriptorToSourceUtils.descriptorToDeclaration(descriptor)) return

        val args = callExpression.getValueArguments()

        val namedArguments = args.filter { arg -> arg is JetValueArgument && arg.getArgumentName()?.getText() == parameter.getName() }
        if (!namedArguments.isEmpty()) {
            usages.add(SafeDeleteValueArgumentListUsageInfo(namedArguments.first(), parameter))
            return
        }

        val originalParameter = parameter.unwrapped
        if (originalParameter == null) return

        val parameterIndex = originalParameter.parameterIndex()
        if (parameterIndex < 0) return

        val argCount = args.size()
        if (parameterIndex < argCount) {
            usages.add(SafeDeleteValueArgumentListUsageInfo((args.get(parameterIndex) as JetValueArgument), parameter))
        } else {
            val lambdaArgs = callExpression.getFunctionLiteralArguments()
            val lambdaIndex = parameterIndex - argCount
            if (lambdaIndex < lambdaArgs.size()) {
                usages.add(SafeDeleteReferenceSimpleDeleteUsageInfo(lambdaArgs.get(lambdaIndex), parameter, true))
            }
        }
    }
}
