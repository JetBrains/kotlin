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

package org.jetbrains.kotlin.android.inspection

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.*
import com.intellij.ide.DataManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.refactoring.rename.RenameHandlerRegistry
import org.jetbrains.kotlin.android.getAndroidFacetForFile
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class IllegalIdentifierInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitElement(element: PsiElement) {
                if (element.node?.elementType != KtTokens.IDENTIFIER) return

                val text = element.text
                // '`' can't be escaped now
                if (!text.startsWith('`') || !text.endsWith('`')) return

                val unquotedName = KtPsiUtil.unquoteIdentifier(text)
                // This is already an error
                if (unquotedName.isEmpty()) return

                if (!unquotedName.all { isValidDalvikCharacter(it) } && checkAndroidFacet(element)) {
                    holder.registerProblem(element,
                                           "Identifier not allowed in Android projects",
                                           ProblemHighlightType.GENERIC_ERROR,
                                           RenameIdentifierFix())
                }
            }

            fun checkAndroidFacet(element: PsiElement): Boolean {
                return element.getAndroidFacetForFile() != null || ApplicationManager.getApplication().isUnitTestMode
            }

            // https://source.android.com/devices/tech/dalvik/dex-format.html#string-syntax
            fun isValidDalvikCharacter(c: Char) = when (c) {
                in 'A'..'Z' -> true
                in 'a'..'z' -> true
                in '0'..'9' -> true
                '$', '-', '_' -> true
                in '\u00a1' .. '\u1fff' -> true
                in '\u2010' .. '\u2027' -> true
                in '\u2030' .. '\ud7ff' -> true
                in '\ue000' .. '\uffef' -> true
                else -> false
            }
        }
    }

    class RenameIdentifierFix : LocalQuickFix {
        override fun getName() = "Rename"
        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement ?: return
            val file = element.containingFile ?: return
            if (!FileModificationService.getInstance().prepareFileForWrite(file)) return
            val editorManager = FileEditorManager.getInstance(project)
            val editor = editorManager.getSelectedEditor(file.virtualFile) ?: return
            val dataContext = DataManager.getInstance().getDataContext(editor.component)
            val renameHandler = RenameHandlerRegistry.getInstance().getRenameHandler(dataContext)
            renameHandler?.invoke(project, arrayOf(element), dataContext);
        }
    }
}