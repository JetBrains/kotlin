/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.findUsages

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiConstructorCall
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.psi.psiUtil.*
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.BindingContextUtils
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache

fun PsiElement.isConstructorUsage(jetClassOrObject: JetClassOrObject): Boolean {
    fun getCallDescriptor(bindingContext: BindingContext): DeclarationDescriptor? {
        val constructorCalleeExpression = getParentByType(javaClass<JetConstructorCalleeExpression>())
        if (constructorCalleeExpression != null) {
            return bindingContext.get(BindingContext.REFERENCE_TARGET, constructorCalleeExpression.getConstructorReferenceExpression())
        }

        val callExpression = getParentByType(javaClass<JetCallExpression>())
        if (callExpression != null) {
            val callee = callExpression.getCalleeExpression()
            if (callee is JetReferenceExpression) {
                return bindingContext.get(BindingContext.REFERENCE_TARGET, callee)
            }
        }

        return null
    }

    fun checkJavaUsage(): Boolean {
        val call = getParentByType(javaClass<PsiConstructorCall>())
        return call != null && call == getParent()
            && call.resolveConstructor()?.getContainingClass()?.getNavigationElement() == jetClassOrObject
    }

    fun checkKotlinUsage(): Boolean {
        val file = getContainingFile()
        if (file !is JetFile) return false

        val bindingContext = AnalyzerFacadeWithCache.analyzeFileWithCache(file).getBindingContext()

        val descriptor = getCallDescriptor(bindingContext)
        if (descriptor !is ConstructorDescriptor) return false

        return BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor.getContainingDeclaration()) == jetClassOrObject
    }

    return checkJavaUsage() || checkKotlinUsage()
}
