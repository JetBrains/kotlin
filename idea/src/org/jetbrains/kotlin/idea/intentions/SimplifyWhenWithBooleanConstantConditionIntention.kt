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
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.replaceWithBranch
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.isFalseConstant
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.isTrueConstant
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtWhenConditionWithExpression
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression

class SimplifyWhenWithBooleanConstantConditionIntention : SelfTargetingRangeIntention<KtWhenExpression>(KtWhenExpression::class.java, "Simplify when expression") {

    override fun applicabilityRange(element: KtWhenExpression): TextRange? {
        if (element.closeBrace == null) return null
        if (element.subjectExpression != null) return null
        if (element.entries.none { it.isTrueConstantCondition() || it.isFalseConstantCondition() }) return null
        return element.whenKeyword.textRange
    }

    override fun applyTo(element: KtWhenExpression, editor: Editor?) {
        val closeBrace = element.closeBrace ?: return
        val project = editor?.project ?: return
        val factory = KtPsiFactory(project)
        val usedAsExpression = element.isUsedAsExpression(element.analyze())
        element.deleteFalseEntry(usedAsExpression)
        element.replaceTrueEntry(usedAsExpression, closeBrace, factory)
    }
}

private fun KtWhenExpression.deleteFalseEntry(usedAsExpression: Boolean) {
    for (entry in entries) {
        if (entry.isFalseConstantCondition()) {
            entry.delete()
        }
    }

    if (entries.isEmpty() && !usedAsExpression) {
        delete()
    }
    else if (entries.singleOrNull()?.isElse == true) {
        elseExpression?.let { replaceWithBranch(it, usedAsExpression) }
    }
}

private fun KtWhenExpression.replaceTrueEntry(usedAsExpression: Boolean, closeBrace: PsiElement, factory: KtPsiFactory) {
    val trueIndex = entries.indexOfFirst { it.isTrueConstantCondition() }
    if (trueIndex == -1) return

    val expression = entries[trueIndex].expression ?: return

    if (trueIndex == 0) {
        replaceWithBranch(expression, usedAsExpression)
    }
    else {
        val elseEntry = factory.createWhenEntry("else -> ${expression.text}")
        for (index in trueIndex until entries.size) {
            entries[index].delete()
        }
        addBefore(elseEntry, closeBrace)
    }
}

private fun KtWhenEntry.isTrueConstantCondition(): Boolean =
        (conditions.singleOrNull() as? KtWhenConditionWithExpression)?.expression.isTrueConstant()

private fun KtWhenEntry.isFalseConstantCondition(): Boolean =
        (conditions.singleOrNull() as? KtWhenConditionWithExpression)?.expression.isFalseConstant()