/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.cfg.WhenChecker
import org.jetbrains.kotlin.cfg.hasUnknown
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.intentions.ImportAllMembersIntention
import org.jetbrains.kotlin.idea.util.generateWhenBranches
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtWhenConditionWithExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class AddWhenRemainingBranchesFix(
    expression: KtWhenExpression,
    val withImport: Boolean = false
) : KotlinQuickFixAction<KtWhenExpression>(expression) {

    override fun getFamilyName() = text

    override fun getText(): String {
        if (withImport) {
            return KotlinBundle.message("fix.add.remaining.branches.with.star.import")
        } else {
            return KotlinBundle.message("fix.add.remaining.branches")
        }
    }

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        return isAvailable(element)
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        addRemainingBranches(element, withImport)
    }

    companion object : KotlinIntentionActionsFactory() {
        private fun KtWhenExpression.hasEnumSubject(): Boolean {
            val subject = subjectExpression ?: return false
            val descriptor = subject.analyze().getType(subject)?.constructor?.declarationDescriptor ?: return false
            return (descriptor as? ClassDescriptor)?.kind == ClassKind.ENUM_CLASS
        }

        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            val whenExpression = diagnostic.psiElement.getNonStrictParentOfType<KtWhenExpression>() ?: return emptyList()
            val actions = mutableListOf(AddWhenRemainingBranchesFix(whenExpression))
            if (whenExpression.hasEnumSubject()) {
                actions += AddWhenRemainingBranchesFix(whenExpression, withImport = true)
            }
            return actions
        }

        fun isAvailable(element: KtWhenExpression?): Boolean {
            if (element == null) return false
            return element.closeBrace != null &&
                    with(WhenChecker.getMissingCases(element, element.analyze())) { isNotEmpty() && !hasUnknown }
        }

        fun addRemainingBranches(element: KtWhenExpression?, withImport: Boolean = false) {
            if (element == null) return
            val missingCases = WhenChecker.getMissingCases(element, element.analyze())

            generateWhenBranches(element, missingCases)

            ShortenReferences.DEFAULT.process(element)

            if (withImport) {
                importAllEntries(element)
            }
        }

        private fun importAllEntries(element: KtWhenExpression) {
            with(ImportAllMembersIntention) {
                element.entries
                    .map { it.conditions.toList() }
                    .flatten()
                    .firstNotNullOfOrNull {
                        (it as? KtWhenConditionWithExpression)?.expression as? KtDotQualifiedExpression
                    }?.importReceiverMembers()
            }
        }
    }
}
