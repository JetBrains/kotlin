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
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetChangeInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetParameterInfo
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.idea.util.ShortenReferences.Options

// Explicit reference to function parameter or outer this
public abstract class JetExplicitReferenceUsage<T : KtElement>(element: T) : JetUsageInfo<T>(element) {
    abstract fun getReplacementText(changeInfo: JetChangeInfo): String

    protected open fun processReplacedElement(element: KtElement) {

    }

    override fun processUsage(changeInfo: JetChangeInfo, element: T, allUsages: Array<out UsageInfo>): Boolean {
        val newElement = KtPsiFactory(element.getProject()).createExpression(getReplacementText(changeInfo))
        val elementToReplace = (element.getParent() as? KtThisExpression) ?: element
        processReplacedElement(elementToReplace.replace(newElement) as KtElement)
        return false
    }
}

public class JetParameterUsage(
        element: KtElement,
        private val parameterInfo: JetParameterInfo,
        val containingCallable: JetCallableDefinitionUsage<*>
) : JetExplicitReferenceUsage<KtElement>(element) {
    override fun processReplacedElement(element: KtElement) {
        val qualifiedExpression = element.getParent() as? KtQualifiedExpression
        val elementToShorten = if (qualifiedExpression?.getReceiverExpression() == element) qualifiedExpression!! else element
        elementToShorten.addToShorteningWaitSet(Options(removeThis = true, removeThisLabels = true))
    }

    override fun getReplacementText(changeInfo: JetChangeInfo): String {
        if (changeInfo.receiverParameterInfo != parameterInfo) return parameterInfo.getInheritedName(containingCallable)

        val newName = changeInfo.getNewName()
        if (KotlinNameSuggester.isIdentifier(newName)) return "this@$newName"

        return "this"
    }
}

public class JetNonQualifiedOuterThisUsage(
        element: KtThisExpression,
        val targetDescriptor: DeclarationDescriptor
) : JetExplicitReferenceUsage<KtThisExpression>(element) {
    override fun processReplacedElement(element: KtElement) {
        element.addToShorteningWaitSet(Options(removeThisLabels = true))
    }

    override fun getReplacementText(changeInfo: JetChangeInfo): String = "this@${targetDescriptor.getName().asString()}"
}