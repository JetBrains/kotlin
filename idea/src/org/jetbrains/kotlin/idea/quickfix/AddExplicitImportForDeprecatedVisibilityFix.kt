/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

internal class AddExplicitImportForDeprecatedVisibilityFix(expression: KtElement, private val targetFqName: FqName) :
    KotlinQuickFixAction<KtElement>(expression) {
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