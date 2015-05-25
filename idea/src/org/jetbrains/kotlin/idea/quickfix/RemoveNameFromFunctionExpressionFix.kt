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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.JetNodeTypes
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionFactory
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionForFirstParentOfType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.resolve.BindingContext

public class RemoveNameFromFunctionExpressionFix(element: JetNamedFunction) : JetIntentionAction<JetNamedFunction>(element), CleanupFix {
    override fun getText(): String = "Remove identifier from function expression"
    override fun getFamilyName(): String = getText()

    override fun invoke(project: Project, editor: Editor?, file: JetFile) = removeNameFromFunction(element)

    companion object : JetSingleIntentionActionFactory() {

        override fun createAction(diagnostic: Diagnostic) =
                diagnostic.createIntentionForFirstParentOfType(::RemoveNameFromFunctionExpressionFix)

        public fun createWholeProjectFixFactory(): JetSingleIntentionActionFactory = createIntentionFactory {
            JetWholeProjectForEachElementOfTypeFix.createByPredicate<JetNamedFunction>(
                    predicate = { isFunctionExpression(it) },
                    taskProcessor = { removeNameFromFunction(it) },
                    name = "Remove identifier from function expressions in the whole project"
            )
        }

        private fun isFunctionExpression(function: JetNamedFunction): Boolean {
            var parent = function.getParent()

            while (parent is JetAnnotatedExpression || parent is JetLabeledExpression) {
                parent = parent.getParent()
            }

            return function.isLocal() && parent !is JetBlockExpression
        }

        private fun removeNameFromFunction(function: JetNamedFunction) {
            var wereAutoLabelUsages = false
            val name = function.getNameAsName() ?: return

            function.forEachDescendantOfType<JetReturnExpression> {
                if (!wereAutoLabelUsages && it.getLabelNameAsName() == name) {
                    wereAutoLabelUsages = it.analyze().get(BindingContext.LABEL_TARGET, it.getTargetLabel()) == function
                }
            }

            function.getNameIdentifier()?.delete()

            if (wereAutoLabelUsages) {
                val psiFactory = JetPsiFactory(function)
                val newFunction = psiFactory.createExpressionByPattern("$0@ $1", name, function)
                function.replace(newFunction)
            }
        }
    }
}
