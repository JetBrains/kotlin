/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinChangeInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinParameterInfo
import org.jetbrains.kotlin.idea.core.ShortenReferences.Options
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtThisExpression

// Explicit reference to function parameter or outer this
abstract class KotlinExplicitReferenceUsage<T : KtElement>(element: T) : KotlinUsageInfo<T>(element) {
    abstract fun getReplacementText(changeInfo: KotlinChangeInfo): String

    protected open fun processReplacedElement(element: KtElement) {

    }

    override fun processUsage(changeInfo: KotlinChangeInfo, element: T, allUsages: Array<out UsageInfo>): Boolean {
        val newElement = KtPsiFactory(element.project).createExpression(getReplacementText(changeInfo))
        val elementToReplace = (element.parent as? KtThisExpression) ?: element
        processReplacedElement(elementToReplace.replace(newElement) as KtElement)
        return false
    }
}

class KotlinParameterUsage(
        element: KtElement,
        private val parameterInfo: KotlinParameterInfo,
        val containingCallable: KotlinCallableDefinitionUsage<*>
) : KotlinExplicitReferenceUsage<KtElement>(element) {
    override fun processReplacedElement(element: KtElement) {
        val qualifiedExpression = element.parent as? KtQualifiedExpression
        val elementToShorten = if (qualifiedExpression?.receiverExpression == element) qualifiedExpression else element
        elementToShorten.addToShorteningWaitSet(Options(removeThis = true, removeThisLabels = true))
    }

    override fun getReplacementText(changeInfo: KotlinChangeInfo): String {
        if (changeInfo.receiverParameterInfo != parameterInfo) return parameterInfo.getInheritedName(containingCallable)

        val newName = changeInfo.newName
        if (KotlinNameSuggester.isIdentifier(newName)) return "this@$newName"

        return "this"
    }
}

class KotlinNonQualifiedOuterThisUsage(
        element: KtThisExpression,
        val targetDescriptor: DeclarationDescriptor
) : KotlinExplicitReferenceUsage<KtThisExpression>(element) {
    override fun processReplacedElement(element: KtElement) {
        element.addToShorteningWaitSet(Options(removeThisLabels = true))
    }

    override fun getReplacementText(changeInfo: KotlinChangeInfo): String = "this@${targetDescriptor.name.asString()}"
}