/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.util.createIntentionForFirstParentOfType
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
