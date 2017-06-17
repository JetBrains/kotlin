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

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getExplicitReceiverValue
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ReplaceAddWithPlusAssignIntention : SelfTargetingOffsetIndependentIntention<KtDotQualifiedExpression>(KtDotQualifiedExpression::class.java, "Replace with '+='") {
    override fun isApplicableTo(element: KtDotQualifiedExpression): Boolean {
        if (element.callExpression?.valueArguments?.size != 1) return false

        when (element.calleeName) {
            "add" -> text = "Replace 'add()' with '+='"
            "addAll" -> text = "Replace 'addAll()' with '+='"
            else -> return false
        }

        val context = element.analyze(BodyResolveMode.PARTIAL)
        BindingContextUtils.extractVariableDescriptorFromReference(context, element.receiverExpression)?.let {
            if (it.isVar) return false
        } ?: return false

        val resolvedCall = element.getResolvedCall(context) ?: return false
        val receiverClass = DescriptorUtils.getClassDescriptorForType(resolvedCall.getExplicitReceiverValue()?.type ?: return false)
        return receiverClass.isSubclassOf(DefaultBuiltIns.Instance.mutableCollection)
    }

    override fun applyTo(element: KtDotQualifiedExpression, editor: Editor?) {
        element.replace(KtPsiFactory(element).createExpressionByPattern("$0 += $1", element.receiverExpression,
                                                                        element.callExpression?.valueArguments?.get(0)?.getArgumentExpression() ?: return))
    }
}