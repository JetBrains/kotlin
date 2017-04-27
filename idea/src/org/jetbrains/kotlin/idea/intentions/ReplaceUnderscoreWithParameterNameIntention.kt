/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch

class ReplaceUnderscoreWithParameterNameIntention :
        SelfTargetingIntention<PsiNamedElement>(PsiNamedElement::class.java, "Replace '_' with parameter name"), LowPriorityAction {
    override fun isApplicableTo(element: PsiNamedElement, caretOffset: Int): Boolean {
        val parameterName = parameterName(element)
        return parameterName?.let {
            text = "Replace _ with $it"
            return true
        } ?: false
    }

    override fun applyTo(element: PsiNamedElement, editor: Editor?) {
        parameterName(element)?.let { element.setName(it) }
    }

    private fun parameterName(element: PsiNamedElement): String? {
        if (element.name != "_") return null
        return when (element) {
            is KtDestructuringDeclarationEntry -> dataClassParameter(element)?.name?.asString()
            is KtParameter -> lambdaParameterName(element)
            else -> null
        }
    }

    private fun lambdaParameterName(element: KtParameter): String? {
        val fnLiteral = element.parent.parent
        val callExpression = fnLiteral.parent.parent.parent as? KtCallExpression ?: return null
        val context = callExpression.analyze()
        val resolvedCall = callExpression.getResolvedCall(context)
        val lambdaParam = (resolvedCall?.getArgumentMapping(fnLiteral.parent.parent as KtLambdaArgument) as? ArgumentMatch)?.valueParameter
        val idx = element.parent.children.indexOf(element)
        return lambdaParam?.type?.arguments?.get(idx)?.type?.annotations?.findAnnotation(FqName("kotlin.ParameterName"))?.argumentValue("name") as? String
    }

    private fun dataClassParameter(declarationEntry: KtDestructuringDeclarationEntry): ValueParameterDescriptor? {
        val entryIndex = declarationEntry.entryIndex()
        if (entryIndex < 0) return null
        val classDescriptor = declarationEntry.parent.classDescriptor() ?: return null
        if (!classDescriptor.isData) return null
        val primaryParameters = classDescriptor.primaryParameters() ?: return null
        if (entryIndex >= primaryParameters.size) return null
        return primaryParameters[entryIndex]
    }

    private fun KtDestructuringDeclarationEntry.entryIndex() = parent.getChildrenOfType<KtDestructuringDeclarationEntry>().indexOf(this)

    private fun PsiElement.classDescriptor(): ClassDescriptor? {
        if (this !is KtDestructuringDeclaration) return null
        val type = initializer?.let { it.analyze().getType(it) } ?: return null
        return type.constructor.declarationDescriptor as? ClassDescriptor
    }

    private fun ClassDescriptor.primaryParameters() = constructors.firstOrNull { it.isPrimary }?.valueParameters
}