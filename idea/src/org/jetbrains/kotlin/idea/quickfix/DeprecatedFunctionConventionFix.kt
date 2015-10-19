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
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters2
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters3
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression

class DeprecatedFunctionConventionFix(
        element: KtNamedFunction,
        private val newName: String
) : KotlinQuickFixAction<KtNamedFunction>(element), CleanupFix {
    override fun getText(): String = "Rename to '$newName'"
    override fun getFamilyName(): String = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        FilteredRenameProcessor(project, element, newName, false, false).run()
    }

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val (functionDescriptor, newName) = when (diagnostic) {
                is DiagnosticWithParameters3<*, *, *, *> -> Pair(diagnostic.a, diagnostic.c)
                is DiagnosticWithParameters2<*, *, *> -> Pair(diagnostic.a, diagnostic.b)
                else -> Pair(null, null)
            }
            if (functionDescriptor !is FunctionDescriptor || newName !is String) return null

            val element = DescriptorToSourceUtilsIde.getAnyDeclaration(diagnostic.psiFile.project, functionDescriptor)
                    as? KtNamedFunction ?: return null
            return DeprecatedFunctionConventionFix(element, newName)
        }
    }

    private class FilteredRenameProcessor(
            project: Project,
            element: PsiElement,
            newName: String,
            isSearchInComments: Boolean,
            isSearchTextOccurrences: Boolean
    ) : RenameProcessor(project, element, newName, isSearchInComments, isSearchTextOccurrences) {
        override fun findUsages(): Array<out UsageInfo> {
            return super.findUsages().filter {
                it.element !is KtOperationReferenceExpression
            }.toTypedArray()
        }
    }
}
