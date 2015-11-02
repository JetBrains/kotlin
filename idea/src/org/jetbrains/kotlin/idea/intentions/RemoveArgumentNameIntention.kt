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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

public class RemoveArgumentNameIntention
  : SelfTargetingRangeIntention<KtValueArgument>(javaClass(), "Remove argument name") {

    override fun applicabilityRange(element: KtValueArgument): TextRange? {
        if (!element.isNamed()) return null

        val argumentList = element.getParent() as? KtValueArgumentList ?: return null
        val arguments = argumentList.getArguments()
        if (arguments.takeWhile { it != element }.any { it.isNamed() }) return null

        val callExpr = argumentList.getParent() as? KtExpression ?: return null
        val resolvedCall = callExpr.getResolvedCall(callExpr.analyze(BodyResolveMode.PARTIAL)) ?: return null
        val argumentMatch = resolvedCall.getArgumentMapping(element) as? ArgumentMatch ?: return null
        if (argumentMatch.valueParameter.index != arguments.indexOf(element)) return null

        val expression = element.getArgumentExpression() ?: return null
        return TextRange(element.startOffset, expression.startOffset)
    }

    override fun applyTo(element: KtValueArgument, editor: Editor) {
        val newArgument = KtPsiFactory(element).createArgument(element.getArgumentExpression()!!, null, element.getSpreadElement() != null)
        element.replace(newArgument)
    }
}