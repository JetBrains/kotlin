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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Comparing
import com.intellij.psi.*
import com.intellij.refactoring.rename.inplace.MemberInplaceRenameHandler
import com.intellij.refactoring.rename.inplace.MemberInplaceRenamer
import com.intellij.refactoring.rename.inplace.VariableInplaceRenamer
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.unquote
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLabelReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class KotlinMemberInplaceRenameHandler : MemberInplaceRenameHandler() {
    private class RenamerImpl(
            elementToRename: PsiNamedElement,
            substitutedElement: PsiElement?,
            editor: Editor,
            currentName: String,
            oldName: String
    ) : MemberInplaceRenamer(elementToRename, substitutedElement, editor, currentName, oldName) {
        override fun acceptReference(reference: PsiReference): Boolean {
            val refElement = reference.element
            val textRange = reference.rangeInElement
            val referenceText = refElement.text.substring(textRange.startOffset, textRange.endOffset).unquote()
            return referenceText == myElementToRename.name
        }

        override fun createInplaceRenamerToRestart(variable: PsiNamedElement, editor: Editor, initialName: String): VariableInplaceRenamer {
            return RenamerImpl(variable, substituted, editor, initialName, myOldName)
        }
    }

    override fun createMemberRenamer(element: PsiElement, elementToRename: PsiNameIdentifierOwner, editor: Editor): MemberInplaceRenamer {
        val currentName = elementToRename.nameIdentifier?.text ?: ""
        return RenamerImpl(elementToRename, element, editor, currentName, currentName)
    }

    override fun isAvailable(element: PsiElement?, editor: Editor, file: PsiFile): Boolean {
        if (element !is KtElement) return false
        if (!super.isAvailable(element, editor, file)) return false

        val referenceExpression = AbstractReferenceSubstitutionRenameHandler.getReferenceExpression(file, editor.caretModel.offset) ?: return true
        if (referenceExpression is KtLabelReferenceExpression) return false
        if (referenceExpression.mainReference.getImportAlias() != null) return false
        if (referenceExpression.analyze(BodyResolveMode.PARTIAL)[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, referenceExpression] != null) return false

        return true
    }
}
