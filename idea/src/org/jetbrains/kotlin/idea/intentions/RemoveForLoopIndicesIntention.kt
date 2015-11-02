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

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.editor.fixers.range
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

public class RemoveForLoopIndicesInspection : IntentionBasedInspection<KtForExpression>(
        listOf(IntentionBasedInspection.IntentionData(RemoveForLoopIndicesIntention())),
        "Index is not used in the loop body",
        javaClass()
) {
    override val problemHighlightType: ProblemHighlightType
        get() = ProblemHighlightType.LIKE_UNUSED_SYMBOL
}

public class RemoveForLoopIndicesIntention : SelfTargetingRangeIntention<KtForExpression>(javaClass(), "Remove indices in 'for' loop") {
    private val WITH_INDEX_FQ_NAME = "kotlin.withIndex"

    override fun applicabilityRange(element: KtForExpression): TextRange? {
        val loopRange = element.loopRange as? KtDotQualifiedExpression ?: return null
        val multiParameter = element.multiParameter ?: return null
        if (multiParameter.entries.size() != 2) return null

        val bindingContext = element.analyze(BodyResolveMode.PARTIAL)

        val resolvedCall = loopRange.getResolvedCall(bindingContext)
        if (resolvedCall?.resultingDescriptor?.fqNameUnsafe?.asString() != WITH_INDEX_FQ_NAME) return null

        val indexVar = multiParameter.entries[0]
        if (ReferencesSearch.search(indexVar).any()) return null

        return indexVar.nameIdentifier?.range
    }

    override fun applyTo(element: KtForExpression, editor: Editor) {
        val multiParameter = element.multiParameter!!
        val loopRange = element.loopRange as KtDotQualifiedExpression

        val elementVar = multiParameter.entries[1]
        val loop = KtPsiFactory(element).createExpressionByPattern("for ($0 in _) {}", elementVar.text) as KtForExpression
        multiParameter.replace(loop.loopParameter!!)

        loopRange.replace(loopRange.receiverExpression)
    }
}
