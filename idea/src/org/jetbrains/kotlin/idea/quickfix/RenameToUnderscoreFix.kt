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

            if (declaration?.nameIdentifier == null ||
                !declaration.languageVersionSettings.supportsFeature(LanguageFeature.SingleUnderscoreForParameterName)) {
                return null
            }

            return RenameToUnderscoreFix(declaration)
        }
    }
}
