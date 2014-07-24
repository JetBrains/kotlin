/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.plugin.JetBundle
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns

public class ConvertToBlockBodyAction : PsiElementBaseIntentionAction() {
    override fun getFamilyName(): String = JetBundle.message("convert.to.block.body.action.family.name")

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        setText(JetBundle.message("convert.to.block.body.action.name"))
        return findDeclaration(element) != null
    }

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val declaration = findDeclaration(element)!!
        val body = declaration.getBodyExpression()!!

        fun generateBody(returnsValue: Boolean): JetExpression {
            val bodyType = expressionType(body)
            val needReturn = returnsValue &&
                    (bodyType == null || (!KotlinBuiltIns.getInstance().isUnit(bodyType) && !KotlinBuiltIns.getInstance().isNothing(bodyType)))

            val oldBodyText = body.getText()!!
            val newBodyText = if (needReturn) "return ${oldBodyText}" else oldBodyText
            return JetPsiFactory(declaration).createFunctionBody(newBodyText)
        }

        if (declaration is JetNamedFunction) {
            val returnType = functionReturnType(declaration)!!
            if (!declaration.hasDeclaredReturnType() && !KotlinBuiltIns.getInstance().isUnit(returnType)) {
                specifyTypeExplicitly(declaration, returnType)
            }

            val newBody = generateBody(!KotlinBuiltIns.getInstance().isUnit(returnType) && !KotlinBuiltIns.getInstance().isNothing(returnType))

            declaration.getEqualsToken()!!.delete()
            body.replace(newBody)
        }
        else if (declaration is JetPropertyAccessor) {
            val newBody = generateBody(declaration.isGetter())
            declaration.getEqualsToken()!!.delete()
            body.replace(newBody)
        }
        else {
            throw RuntimeException("Unknown declaration type: $declaration")
        }
    }

    private fun findDeclaration(element: PsiElement): JetDeclarationWithBody? {
        val declaration = PsiTreeUtil.getParentOfType(element, javaClass<JetDeclarationWithBody>())
        if (declaration == null || declaration is JetFunctionLiteral || declaration.hasBlockBody()) return null
        val body = declaration.getBodyExpression()
        if (body == null) return null

        return when (declaration) {
            is JetNamedFunction -> {
                val returnType = functionReturnType(declaration)
                if (returnType == null) return null
                if (!declaration.hasDeclaredReturnType() && returnType.isError()) return null // do not convert when type is implicit and unknown
                declaration
            }

            is JetPropertyAccessor -> declaration

            else -> throw RuntimeException("Unknown declaration type: $declaration")
        }
    }
}
