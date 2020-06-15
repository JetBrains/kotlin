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

package org.jetbrains.kotlin.idea.quickfix.replaceWith

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.codeInliner.UsageReplacementStrategy
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.core.targetDescriptors
import org.jetbrains.kotlin.idea.quickfix.CleanupFix
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny

class DeprecatedSymbolUsageFix(
    element: KtReferenceExpression,
    replaceWith: ReplaceWith
) : DeprecatedSymbolUsageFixBase(element, replaceWith), CleanupFix, HighPriorityAction {

    override fun getFamilyName() = KotlinBundle.message("replace.deprecated.symbol.usage")

    override fun getText() = KotlinBundle.message("replace.with.0", replaceWith.pattern)

    override fun invoke(replacementStrategy: UsageReplacementStrategy, project: Project, editor: Editor?) {
        val element = element ?: return
        val result = replacementStrategy.createReplacer(element)?.invoke()
        if (result != null) {
            val offset = (result.getCalleeExpressionIfAny() ?: result).textOffset
            editor?.moveCaret(offset)
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val (referenceExpression, replacement) = extractDataFromDiagnostic(diagnostic, false) ?: return null
            return DeprecatedSymbolUsageFix(referenceExpression, replacement)
        }

        fun isImportToBeRemoved(import: KtImportDirective): Boolean {
            if (import.isAllUnder) return false

            val targetDescriptors = import.targetDescriptors()
            if (targetDescriptors.isEmpty()) return false

            return targetDescriptors.all {
                fetchReplaceWithPattern(it, import.project, null, false) != null
            }
        }
    }
}
