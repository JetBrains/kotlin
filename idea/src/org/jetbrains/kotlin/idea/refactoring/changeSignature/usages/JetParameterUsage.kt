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

import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetChangeInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetParameterInfo
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.psi.JetQualifiedExpression
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetThisExpression
import org.jetbrains.kotlin.idea.util.ShortenReferences.Options

// Explicit reference to function parameter or outer this
public abstract class JetExplicitReferenceUsage<T: JetElement>(element: T) : JetUsageInfo<T>(element) {
    abstract fun getReplacementText(changeInfo: JetChangeInfo): String

    protected open fun processReplacedElement(element: JetElement) {

    }

    override fun processUsage(changeInfo: JetChangeInfo, element: T): Boolean {
        val newElement = JetPsiFactory(element.getProject()).createExpression(getReplacementText(changeInfo))
        processReplacedElement(element.replace(newElement) as JetElement)
        return false
    }
}

public class JetParameterUsage(
        element: JetSimpleNameExpression,
        private val parameterInfo: JetParameterInfo,
        val containingFunction: JetFunctionDefinitionUsage<*>
) : JetExplicitReferenceUsage<JetSimpleNameExpression>(element) {
    override fun processReplacedElement(element: JetElement) {
        val qualifiedExpression = element.getParent() as? JetQualifiedExpression
        val elementToShorten = if (qualifiedExpression?.getReceiverExpression() == element) qualifiedExpression!! else element
        elementToShorten.addToShorteningWaitSet(Options(removeThis = true, removeThisLabels = true))
    }

    override fun getReplacementText(changeInfo: JetChangeInfo): String =
            if (changeInfo.receiverParameterInfo != parameterInfo) {
                parameterInfo.getInheritedName(containingFunction)
            } else "this@${containingFunction.getOriginalFunctionDescriptor().getName().asString()}"
}

public class JetNonQualifiedOuterThisUsage(
        element: JetThisExpression,
        val targetDescriptor: DeclarationDescriptor
) : JetExplicitReferenceUsage<JetThisExpression>(element) {
    override fun processReplacedElement(element: JetElement) {
        element.addToShorteningWaitSet(Options(removeThisLabels = true))
    }

    override fun getReplacementText(changeInfo: JetChangeInfo): String = "this@${targetDescriptor.getName().asString()}"
}