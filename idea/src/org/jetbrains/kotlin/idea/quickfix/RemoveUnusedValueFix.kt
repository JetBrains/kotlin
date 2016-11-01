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

import com.intellij.codeInsight.daemon.QuickFixBundle
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.jetbrains.kotlin.cfg.pseudocode.getContainingPseudocode
import org.jetbrains.kotlin.cfg.pseudocode.sideEffectFree
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class RemoveUnusedValueFix(expression: KtBinaryExpression) : KotlinQuickFixAction<KtBinaryExpression>(expression) {
    enum class RemoveMode {
        REMOVE_ALL, KEEP_INITIALIZE, CANCEL
    }

    private fun showDialog(variable: KtProperty): RemoveMode {
        if (ApplicationManager.getApplication().isUnitTestMode) return RemoveMode.KEEP_INITIALIZE

        val message =
                """<html>
                    <body>
                        There are possible side effects found in expressions assigned to the variable '${variable.name}'<br>
                        You can:<br>
                        -&nbsp;<b>Remove</b> the entire assignment, or<br>
                        -&nbsp;<b>Transform</b> assignment right-hand side into the statement on its own.<br>
                    </body>
                </html>"""
        val exitCode = Messages.showYesNoCancelDialog(
                variable.project,
                message,
                QuickFixBundle.message("side.effects.warning.dialog.title"),
                QuickFixBundle.message("side.effect.action.remove"),
                QuickFixBundle.message("side.effect.action.transform"),
                QuickFixBundle.message("side.effect.action.cancel"),
                Messages.getWarningIcon()
        )
        return RemoveMode.values()[exitCode]
    }

    override fun getFamilyName() = "Remove redundant assignment"

    override fun getText() = familyName

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val lhs = element.left as? KtSimpleNameExpression ?: return
        val rhs = element.right ?: return
        val variable = lhs.mainReference.resolve() as? KtProperty ?: return
        val pseudocode = rhs.getContainingPseudocode(element.analyze(BodyResolveMode.PARTIAL))
        val isSideEffectFree = pseudocode?.getElementValue(rhs)?.createdAt?.sideEffectFree ?: false
        var removeMode = RemoveMode.REMOVE_ALL
        if (!isSideEffectFree) {
            removeMode = showDialog(variable)
        }

        when (removeMode) {
            RemoveMode.REMOVE_ALL -> element.delete()
            RemoveMode.KEEP_INITIALIZE -> element.replace(rhs)
            else -> {}
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val expression = Errors.UNUSED_VALUE.cast(diagnostic).psiElement
            if (!KtPsiUtil.isAssignment(expression)) return null
            if (expression.left !is KtSimpleNameExpression) return null
            return RemoveUnusedValueFix(expression)
        }
    }
}