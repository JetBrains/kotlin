/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.openapi.editor.Editor
import com.intellij.psi.*
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler
import com.intellij.refactoring.rename.inplace.VariableInplaceRenamer
import org.jetbrains.kotlin.idea.core.unquote
import org.jetbrains.kotlin.psi.*

open class KotlinVariableInplaceRenameHandler : VariableInplaceRenameHandler() {
    companion object {
        fun isInplaceRenameAvailable(element: PsiElement): Boolean {
            when (element) {
                is KtTypeParameter -> return true
                is KtProperty -> {
                    if (element.isLocal) return true
                }
                is KtDestructuringDeclarationEntry -> return true
                is KtParameter -> {
                    val parent = element.parent
                    if (parent is KtForExpression) {
                        return true
                    }
                    if (parent is KtParameterList) {
                        val grandparent = parent.parent
                        return grandparent is KtCatchClause || grandparent is KtFunctionLiteral
                    }
                }
                is KtLabeledExpression, is KtImportAlias -> return true
            }
            return false
        }
    }

    protected open class RenamerImpl : VariableInplaceRenamer {
        constructor(elementToRename: PsiNamedElement, editor: Editor): super(elementToRename, editor)
        constructor(
            elementToRename: PsiNamedElement,
            editor: Editor,
            currentName: String,
            oldName: String
        ) : super(elementToRename, editor, editor.project, currentName, oldName)

        override fun acceptReference(reference: PsiReference): Boolean {
            val refElement = reference.element
            val textRange = reference.rangeInElement
            val referenceText = refElement.text.substring(textRange.startOffset, textRange.endOffset).unquote()
            return referenceText == myElementToRename.name
        }

        override fun startsOnTheSameElement(handler: RefactoringActionHandler?, element: PsiElement?): Boolean {
            return variable == element && (handler is VariableInplaceRenameHandler || handler is KotlinRenameDispatcherHandler)
        }

        override fun createInplaceRenamerToRestart(variable: PsiNamedElement, editor: Editor, initialName: String): VariableInplaceRenamer {
            return RenamerImpl(variable, editor, initialName, myOldName)
        }
    }

    override fun createRenamer(elementToRename: PsiElement, editor: Editor): VariableInplaceRenamer? {
        val currentElementToRename = elementToRename as PsiNameIdentifierOwner
        val currentName = currentElementToRename.nameIdentifier?.text ?: ""
        return RenamerImpl(currentElementToRename, editor, currentName, currentName)
    }

    override public fun isAvailable(element: PsiElement?, editor: Editor, file: PsiFile) =
        editor.settings.isVariableInplaceRenameEnabled && element != null && isInplaceRenameAvailable(element)
}