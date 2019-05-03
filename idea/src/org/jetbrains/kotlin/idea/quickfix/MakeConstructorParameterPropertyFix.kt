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
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinValVar
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.*

class MakeConstructorParameterPropertyFix(
    element: KtParameter, private val kotlinValVar: KotlinValVar, className: String?
) : KotlinQuickFixAction<KtParameter>(element) {
    override fun getFamilyName() = "Make constructor parameter a property"

    private val suffix = if (className != null) " in class '$className'" else ""

    override fun getText() = "Make constructor parameter a property$suffix"

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        val element = element ?: return false
        return !element.hasValOrVar()
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        element.addBefore(kotlinValVar.createKeyword(KtPsiFactory(project))!!, element.nameIdentifier)
        element.addModifier(KtTokens.PRIVATE_KEYWORD)
        element.visibilityModifier()?.let { private ->
            editor?.apply {
                selectionModel.setSelection(private.startOffset, private.endOffset)
                caretModel.moveToOffset(private.endOffset)
            }
        }
    }

    companion object Factory : KotlinIntentionActionsFactory() {

        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            val ktReference = Errors.UNRESOLVED_REFERENCE.cast(diagnostic).a as? KtNameReferenceExpression ?: return emptyList()

            val valOrVar = if (ktReference.getAssignmentByLHS() != null) KotlinValVar.Var else KotlinValVar.Val

            val ktParameter = ktReference.getPrimaryConstructorParameterWithSameName() ?: return emptyList()
            val containingClass = ktParameter.containingClass()!!
            val className = if (containingClass != ktReference.containingClass()) containingClass.nameAsSafeName.asString() else null

            return listOf(MakeConstructorParameterPropertyFix(ktParameter, valOrVar, className))
        }
    }
}

fun KtNameReferenceExpression.getPrimaryConstructorParameterWithSameName(): KtParameter? {
    return nonStaticOuterClasses()
        .mapNotNull { it.primaryConstructor?.valueParameters?.firstOrNull { it.name == getReferencedName() } }
        .firstOrNull()
}