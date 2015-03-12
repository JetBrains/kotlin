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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

public class ConvertToBlockBodyAction : PsiElementBaseIntentionAction() {
    override fun getFamilyName(): String = JetBundle.message("convert.to.block.body.action.family.name")

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        setText(JetBundle.message("convert.to.block.body.action.name"))
        return findDeclaration(element) != null
    }

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        convert(findDeclaration(element)!!)
    }

    default object {
        fun convert(declaration: JetDeclarationWithBody): JetDeclarationWithBody {
            val body = declaration.getBodyExpression()!!

            fun generateBody(returnsValue: Boolean): JetExpression {
                val bodyType = expressionType(body)
                val needReturn = returnsValue &&
                                 (bodyType == null || (!KotlinBuiltIns.isUnit(bodyType) && !KotlinBuiltIns.isNothing(bodyType)))

                val oldBodyText = body.getText()!!
                val newBodyText = if (needReturn) "return ${oldBodyText}" else oldBodyText
                return JetPsiFactory(declaration).createFunctionBody(newBodyText)
            }

            val newBody = when (declaration) {
                is JetNamedFunction -> {
                    val returnType = functionReturnType(declaration)!!
                    if (!declaration.hasDeclaredReturnType() && !KotlinBuiltIns.isUnit(returnType)) {
                        specifyTypeExplicitly(declaration, returnType)
                    }
                    generateBody(!KotlinBuiltIns.isUnit(returnType) && !KotlinBuiltIns.isNothing(returnType))
                }

                is JetPropertyAccessor -> generateBody(declaration.isGetter())

                else -> throw RuntimeException("Unknown declaration type: $declaration")
            }

            declaration.getEqualsToken()!!.delete()
            body.replace(newBody)
            return declaration
        }

        private fun findDeclaration(element: PsiElement): JetDeclarationWithBody? {
            val declaration = element.getStrictParentOfType<JetDeclarationWithBody>()
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
}
