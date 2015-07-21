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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.psi.JetDotQualifiedExpression
import org.jetbrains.kotlin.psi.JetForExpression
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils

public class RemoveForLoopIndicesIntention : JetSelfTargetingIntention<JetForExpression>(
       javaClass(), "Remove indices in for-loop") {
    override fun applyTo(element: JetForExpression, editor: Editor) {
        val parameter = element.getMultiParameter()!!
        val range = element.getLoopRange() as JetDotQualifiedExpression
        val parameters = parameter.getEntries()

        val loop = JetPsiFactory(element).createExpression("for (${parameters[1].getText()} in _) {}") as JetForExpression
        parameter.replace(loop.getLoopParameter()!!)

        range.replace(range.getReceiverExpression())
    }

    override fun isApplicableTo(element: JetForExpression, caretOffset: Int): Boolean {
        val multiParameter = element.getMultiParameter() ?: return false
        if (multiParameter.getEntries().size() != 2) return false
        val range = element.getLoopRange() as? JetDotQualifiedExpression ?: return false
        val selector = range.getSelectorExpression() as? JetCallExpression ?: return false

        if (!selector.textMatches("withIndex()")) return false

        val body = element.getBody()
        if (body != null && caretOffset >= body.getTextRange().getStartOffset()) return false

        val bindingContext = element.analyze()
        val call = bindingContext[BindingContext.CALL, selector.getCalleeExpression()] ?: return false
        val callResolution = bindingContext[BindingContext.RESOLVED_CALL, call] ?: return false
        val fqName = DescriptorUtils.getFqNameSafe(callResolution.getCandidateDescriptor())
        if (fqName.toString() != "kotlin.withIndex") return false

        val indexVar = multiParameter.getEntries()[0]
        return ReferencesSearch.search(indexVar).findFirst() == null
    }
}
