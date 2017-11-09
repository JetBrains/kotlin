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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionForFirstParentOfType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.resolve.BindingContext

class RemoveNameFromFunctionExpressionFix(element: KtNamedFunction) : KotlinQuickFixAction<KtNamedFunction>(element), CleanupFix {
    override fun getText(): String = "Remove identifier from anonymous function"
    override fun getFamilyName(): String = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        removeNameFromFunction(element ?: return)
    }

    companion object : KotlinSingleIntentionActionFactory() {

        override fun createAction(diagnostic: Diagnostic) =
                diagnostic.createIntentionForFirstParentOfType(::RemoveNameFromFunctionExpressionFix)

        private fun removeNameFromFunction(function: KtNamedFunction) {
            var wereAutoLabelUsages = false
            val name = function.nameAsName ?: return

            function.forEachDescendantOfType<KtReturnExpression> {
                if (!wereAutoLabelUsages && it.getLabelNameAsName() == name) {
                    wereAutoLabelUsages = it.analyze().get(BindingContext.LABEL_TARGET, it.getTargetLabel()) == function
                }
            }

            function.nameIdentifier?.delete()

            if (wereAutoLabelUsages) {
                val psiFactory = KtPsiFactory(function)
                val newFunction = psiFactory.createExpressionByPattern("$0@ $1", name, function)
                function.replace(newFunction)
            }
        }
    }
}
