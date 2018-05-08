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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

internal class AddExplicitImportForDeprecatedVisibilityFix(expression: KtElement, private val targetFqName: FqName) : KotlinQuickFixAction<KtElement>(expression) {
    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val targetDescriptor = file.resolveImportReference(targetFqName).singleOrNull() ?: return
        ImportInsertHelper.getInstance(project).importDescriptor(file, targetDescriptor)
    }

    override fun getFamilyName(): String = text

    override fun getText(): String = "Add explicit import"

    object Factory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            if (diagnostic.factory != Errors.DEPRECATED_ACCESS_BY_SHORT_NAME) return null
            val castedDiagnostic = Errors.DEPRECATED_ACCESS_BY_SHORT_NAME.cast(diagnostic)

            val soonToBeDeprecatedType = castedDiagnostic.psiElement
            val importableFqNameOfTargetDescriptor = castedDiagnostic.a.importableFqName ?: return null

            return AddExplicitImportForDeprecatedVisibilityFix(soonToBeDeprecatedType, importableFqNameOfTargetDescriptor)
        }
    }
}