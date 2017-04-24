/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.intentions.SpecifyTypeExplicitlyIntention
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.types.isError

class SpecifyTypeExplicitlyFix : PsiElementBaseIntentionAction() {
    override fun getFamilyName() = "Specify type explicitly"

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        if (!FileModificationService.getInstance().preparePsiElementForWrite(element)) return

        val declaration = declarationByElement(element)!!
        val type = SpecifyTypeExplicitlyIntention.getTypeForDeclaration(declaration)
        SpecifyTypeExplicitlyIntention.addTypeAnnotation(editor, declaration, type)
    }

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        val declaration = declarationByElement(element)
        if (declaration is KtProperty) {
            text = "Specify type explicitly"
        }
        else if (declaration is KtNamedFunction) {
            text = "Specify return type explicitly"
        }
        else {
            return false
        }

        return !SpecifyTypeExplicitlyIntention.getTypeForDeclaration(declaration).isError
    }

    private fun declarationByElement(element: PsiElement): KtCallableDeclaration? {
        return PsiTreeUtil.getParentOfType(element, KtProperty::class.java, KtNamedFunction::class.java) as KtCallableDeclaration?
    }
}
