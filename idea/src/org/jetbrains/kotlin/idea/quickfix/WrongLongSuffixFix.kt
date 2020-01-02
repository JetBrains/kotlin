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
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory

class WrongLongSuffixFix(element: KtConstantExpression) : KotlinQuickFixAction<KtConstantExpression>(element) {
    private val corrected = element.text.trimEnd('l') + 'L'

    override fun getText() = "Change to '$corrected'"
    override fun getFamilyName() = "Change to correct long suffix 'L'"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element?.replace(KtPsiFactory(project).createExpression(corrected))
    }

    companion object Factory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction {
            val casted = Errors.WRONG_LONG_SUFFIX.cast(diagnostic)
            return WrongLongSuffixFix(casted.psiElement)
        }
    }
}