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
import com.intellij.refactoring.rename.RenameProcessor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.types.expressions.OperatorConventions.REM_TO_MOD_OPERATION_NAMES

class RenameModToRemFix(element: KtNamedFunction, val newName: Name) : KotlinQuickFixAction<KtNamedFunction>(element) {
    override fun getText(): String = "Rename to '$newName'"

    override fun getFamilyName(): String = text

    override fun startInWriteAction(): Boolean = false

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        RenameProcessor(project, element ?: return, newName.asString(), false, false).run()
    }

    companion object Factory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            if (diagnostic.factory != Errors.DEPRECATED_BINARY_MOD && diagnostic.factory != Errors.FORBIDDEN_BINARY_MOD) return null

            val operatorMod = diagnostic.psiElement.getNonStrictParentOfType<KtNamedFunction>() ?: return null
            val newName = REM_TO_MOD_OPERATION_NAMES.inverse()[operatorMod.nameAsName] ?: return null
            return RenameModToRemFix(operatorMod, newName)
        }
    }
}