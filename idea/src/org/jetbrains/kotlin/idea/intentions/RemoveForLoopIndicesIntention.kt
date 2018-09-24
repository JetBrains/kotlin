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
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.editor.fixers.range
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe

class RemoveForLoopIndicesInspection : IntentionBasedInspection<KtForExpression>(
        RemoveForLoopIndicesIntention::class,
        "Index is not used in the loop body"
) {
    override fun problemHighlightType(element: KtForExpression): ProblemHighlightType = ProblemHighlightType.LIKE_UNUSED_SYMBOL
}

class RemoveForLoopIndicesIntention : SelfTargetingRangeIntention<KtForExpression>(KtForExpression::class.java, "Remove indices in 'for' loop") {
    private val WITH_INDEX_NAME = "withIndex"
    private val WITH_INDEX_FQ_NAMES = sequenceOf("collections", "sequences", "text", "ranges").map { "kotlin.$it.$WITH_INDEX_NAME" }.toSet()

    override fun applicabilityRange(element: KtForExpression): TextRange? {
        val loopRange = element.loopRange as? KtDotQualifiedExpression ?: return null
        val multiParameter = element.destructuringDeclaration ?: return null
        if (multiParameter.entries.size != 2) return null

        val resolvedCall = loopRange.resolveToCall()
        if (resolvedCall?.resultingDescriptor?.fqNameUnsafe?.asString() !in WITH_INDEX_FQ_NAMES) return null

        val indexVar = multiParameter.entries[0]
        if (ReferencesSearch.search(indexVar).any()) return null

        return indexVar.nameIdentifier?.range
    }

    override fun applyTo(element: KtForExpression, editor: Editor?) {
        val multiParameter = element.destructuringDeclaration!!
        val loopRange = element.loopRange as KtDotQualifiedExpression

        val elementVar = multiParameter.entries[1]
        val loop = KtPsiFactory(element).createExpressionByPattern("for ($0 in _) {}", elementVar.text) as KtForExpression
        element.loopParameter!!.replace(loop.loopParameter!!)

        loopRange.replace(loopRange.receiverExpression)
    }
}
