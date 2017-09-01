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
package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class RemoveRedundantBackticksInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitKtElement(element: KtElement) {
                super.visitKtElement(element)
                element.findChildrenByType(KtTokens.IDENTIFIER).forEach {
                    if (isRedundantBackticks(it)) {
                        registerProblem(holder, it)
                    }
                }
            }
        }
    }

    private fun isKeyword(text: String): Boolean {
        return (KtTokens.KEYWORDS.types + KtTokens.SOFT_KEYWORDS.types).any { it.toString() == text }
    }

    private fun isRedundantBackticks(element: PsiElement): Boolean {
        return (element.text.startsWith("`") &&
                element.text.endsWith("`") &&
                KotlinNameSuggester.isIdentifier(element.text) &&
                !isKeyword(element.text.removePrefix("`").removeSuffix("`")))
    }

    private fun registerProblem(holder: ProblemsHolder, element: PsiElement) {
        holder.registerProblem(element,
                               "Remove redundant backticks",
                               ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                               RemoveRedundantBackticksQuickFix())
    }

    private fun KtElement.findChildrenByType(elementType: IElementType): List<PsiElement> {
        val EMPTY = emptyList<PsiElement>()
        var result = arrayListOf<PsiElement>()
        var child = node.firstChildNode
        while (child != null) {
            if (elementType == child.elementType) {
                if (result == EMPTY) {
                    result = arrayListOf<PsiElement>()
                }
                result.add(child.psi)
            }
            child = child.treeNext
        }
        return result
    }
}

class RemoveRedundantBackticksQuickFix : LocalQuickFix {
    override fun getName() = "Remove redundant backticks"
    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val factory = KtPsiFactory(project)
        element.replace(factory.createIdentifier(element.text.removePrefix("`").removeSuffix("`")))
    }
}