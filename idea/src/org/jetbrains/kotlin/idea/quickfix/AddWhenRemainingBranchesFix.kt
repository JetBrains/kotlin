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
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.cfg.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.quoteIfNeeded
import org.jetbrains.kotlin.idea.intentions.ImportAllMembersIntention
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

class AddWhenRemainingBranchesFix(
        expression: KtWhenExpression,
        val withImport: Boolean = false
) : KotlinQuickFixAction<KtWhenExpression>(expression) {

    override fun getFamilyName() = text

    override fun getText() = "Add remaining branches" + if (withImport) " with import" else ""

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        val element = element ?: return false
        return super.isAvailable(project, editor, file) && element.closeBrace != null &&
               with(WhenChecker.getMissingCases(element, element.analyze())) { isNotEmpty() && !hasUnknown }
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val missingCases = WhenChecker.getMissingCases(element, element.analyze())

        val whenCloseBrace = element.closeBrace ?: throw AssertionError("isAvailable should check if close brace exist")
        val psiFactory = KtPsiFactory(file)

        for (case in missingCases) {
            val branchConditionText = when (case) {
                UnknownMissingCase, NullMissingCase, is BooleanMissingCase ->
                    case.branchConditionText
                is ClassMissingCase ->
                    if (case.classIsSingleton) {
                        case.classFqName.quoteIfNeeded().asString()
                    }
                    else {
                        "is " + case.classFqName.quoteIfNeeded().asString()
                    }
            }
            val entry = psiFactory.createWhenEntry("$branchConditionText -> TODO()")
            element.addBefore(entry, whenCloseBrace)
        }

        if (withImport) {
            importAllEntries(element)
        }
    }

    private fun importAllEntries(element: KtWhenExpression) {
        with (ImportAllMembersIntention) {
            element.entries
                    .map { it.conditions.toList() }
                    .flatten()
                    .firstNotNullResult {
                        (it as? KtWhenConditionWithExpression)?.expression as? KtDotQualifiedExpression
                    }?.importReceiverMembers()
        }
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
    }
}