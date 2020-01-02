/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

@Suppress("DEPRECATION")
class RemoveForLoopIndicesInspection : IntentionBasedInspection<KtForExpression>(
    RemoveForLoopIndicesIntention::class,
    "Index is not used in the loop body"
) {
    override fun problemHighlightType(element: KtForExpression): ProblemHighlightType = ProblemHighlightType.LIKE_UNUSED_SYMBOL
}

class RemoveForLoopIndicesIntention :
    SelfTargetingRangeIntention<KtForExpression>(KtForExpression::class.java, "Remove indices in 'for' loop") {
    private val WITH_INDEX_NAME = "withIndex"
    private val WITH_INDEX_FQ_NAMES: Set<String> by lazy {
        sequenceOf("collections", "sequences", "text", "ranges").map { "kotlin.$it.$WITH_INDEX_NAME" }.toSet()
    }

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
