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

import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinChangeInfo
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver

public class KotlinPropertyCallUsage(element: KtSimpleNameExpression): KotlinUsageInfo<KtSimpleNameExpression>(element) {
    private val resolvedCall = element.getResolvedCall(element.analyze())

    override fun processUsage(changeInfo: KotlinChangeInfo, element: KtSimpleNameExpression, allUsages: Array<out UsageInfo>): Boolean {
        updateName(changeInfo, element)
        updateReceiver(changeInfo, element)
        return true
    }

    private fun updateName(changeInfo: KotlinChangeInfo, element: KtSimpleNameExpression) {
        if (changeInfo.isNameChanged()) {
            element.mainReference.handleElementRename(changeInfo.getNewName())
        }
    }

    private fun updateReceiver(changeInfo: KotlinChangeInfo, element: KtSimpleNameExpression) {
        val newReceiver = changeInfo.receiverParameterInfo
        val oldReceiver = changeInfo.methodDescriptor.receiver
        if (newReceiver == oldReceiver) return

        val elementToReplace = element.getQualifiedExpressionForSelectorOrThis()

        // Do not add extension receiver to calls with explicit dispatch receiver
        if (newReceiver != null
            && elementToReplace is KtQualifiedExpression
            && resolvedCall?.getDispatchReceiver() is ExpressionReceiver) return

        val replacingElement = newReceiver?.let {
            val psiFactory = KtPsiFactory(getProject())
            val receiver = it.defaultValueForCall ?: psiFactory.createExpression("_")
            psiFactory.createExpressionByPattern("$0.$1", receiver, element)
        } ?: element

        elementToReplace.replace(replacingElement)
    }
}