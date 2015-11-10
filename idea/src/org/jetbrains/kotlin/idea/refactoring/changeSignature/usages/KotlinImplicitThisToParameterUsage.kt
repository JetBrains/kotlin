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
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinChangeInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinParameterInfo
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.util.ShortenReferences.Options

public abstract class KotlinImplicitReceiverUsage(callElement: KtElement): KotlinUsageInfo<KtElement>(callElement) {
    protected abstract fun getNewReceiverText(): String

    protected open fun processReplacedElement(element: KtElement) {

    }

    override fun processUsage(changeInfo: KotlinChangeInfo, element: KtElement, allUsages: Array<out UsageInfo>): Boolean {
        val newQualifiedCall = KtPsiFactory(element.getProject()).createExpression(
                "${getNewReceiverText()}.${element.getText()}"
        ) as KtQualifiedExpression
        processReplacedElement(element.replace(newQualifiedCall) as KtElement)
        return false
    }
}

public class KotlinImplicitThisToParameterUsage(
        callElement: KtElement,
        val parameterInfo: KotlinParameterInfo,
        val containingCallable: KotlinCallableDefinitionUsage<*>
): KotlinImplicitReceiverUsage(callElement) {
    override fun getNewReceiverText(): String = parameterInfo.getInheritedName(containingCallable)

    override fun processReplacedElement(element: KtElement) {
        element.addToShorteningWaitSet(Options(removeThisLabels = true))
    }
}

public class KotlinImplicitThisUsage(
        callElement: KtElement,
        val targetDescriptor: DeclarationDescriptor
): KotlinImplicitReceiverUsage(callElement) {
    override fun getNewReceiverText(): String {
        val name = targetDescriptor.getName()
        return if (name.isSpecial()) "this" else "this@${name.asString()}"
    }

    override fun processReplacedElement(element: KtElement) {
        element.addToShorteningWaitSet(Options(removeThisLabels = true, removeThis = true))
    }
}