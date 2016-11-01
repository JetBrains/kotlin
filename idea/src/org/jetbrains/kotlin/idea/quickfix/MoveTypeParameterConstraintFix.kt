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

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class MoveTypeParameterConstraintFix(element: KtTypeParameter) : KotlinQuickFixAction<KtTypeParameter>(element), CleanupFix {
    override fun getText(): String = "Move type parameter constraint to 'where' clause"
    override fun getFamilyName(): String = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val typeParameterName = element.nameAsName ?: return
        val psiFactory = KtPsiFactory(file)
        val templateClass = psiFactory.buildDeclaration {
            appendFixedText("class A<")
            appendName(typeParameterName)
            appendFixedText("> where ")
            appendName(typeParameterName)
            appendFixedText(":")
            appendTypeReference(element.extendsBound)
        } as KtTypeParameterListOwner
        val templateConstraintList = templateClass.typeConstraintList!!

        val declaration = element.getStrictParentOfType<KtTypeParameterListOwner>() ?: return
        val constraintList = declaration.typeConstraintList ?: return
        constraintList.addAfter(psiFactory.createComma(), null)
        constraintList.addAfter(templateConstraintList.constraints[0], null)

        element.extendsBound?.delete()
        val colon = element.node.findChildByType(KtTokens.COLON)
        colon?.psi?.delete()
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val typeParameter = diagnostic.psiElement as? KtTypeParameter ?: return null
            return MoveTypeParameterConstraintFix(typeParameter)
        }
    }
}
