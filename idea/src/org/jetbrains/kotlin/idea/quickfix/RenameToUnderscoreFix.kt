/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.psi.*

class RenameToUnderscoreFix(element: KtCallableDeclaration) : KotlinQuickFixAction<KtCallableDeclaration>(element) {
    override fun getText() = "Rename to _"
    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element?.nameIdentifier?.replace(KtPsiFactory(project).createIdentifier("_"))
    }

    companion object Factory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val declaration: KtCallableDeclaration? = when (diagnostic.factory) {
                Errors.UNUSED_ANONYMOUS_PARAMETER -> {
                    val parameter = diagnostic.psiElement as? KtParameter
                    val owner = parameter?.parent?.parent

                    if (owner is KtFunctionLiteral || (owner is KtNamedFunction && owner.name == null))
                        parameter
                    else
                        null
                }
                Errors.UNUSED_VARIABLE, Errors.UNUSED_DESTRUCTURED_PARAMETER_ENTRY ->
                    diagnostic.psiElement as? KtDestructuringDeclarationEntry
                else -> null
            }

            if (declaration?.nameIdentifier == null || !declaration.languageVersionSettings
                    .supportsFeature(LanguageFeature.SingleUnderscoreForParameterName)
            ) {
                return null
            }

            return RenameToUnderscoreFix(declaration)
        }
    }
}
