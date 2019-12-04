/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory

object AddSpreadOperatorForArrayAsVarargAfterSamFixFactory : KotlinSingleIntentionActionFactory() {
    public override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val diagnosticWithParameters = Errors.TYPE_INFERENCE_CANDIDATE_WITH_SAM_AND_VARARG.cast(diagnostic)
        val argument = diagnosticWithParameters.psiElement

        return AddSpreadOperatorForArrayAsVarargAfterSamFix(argument)
    }
}

class AddSpreadOperatorForArrayAsVarargAfterSamFix(element: PsiElement) : KotlinQuickFixAction<PsiElement>(element) {
    override fun getFamilyName() = "Add a spread operator before an array passing as vararg"

    override fun getText() = familyName

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return

        element.addBefore(KtPsiFactory(file).createStar(), element.firstChild)
    }
}