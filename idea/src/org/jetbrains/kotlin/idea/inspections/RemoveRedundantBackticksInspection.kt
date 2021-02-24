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

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.SharedImplUtil
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.core.unquote
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.canPlaceAfterSimpleNameEntry
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isIdentifier

class RemoveRedundantBackticksInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitKtElement(element: KtElement) {
                super.visitKtElement(element)
                SharedImplUtil.getChildrenOfType(element.node, KtTokens.IDENTIFIER).forEach {
                    if (isRedundantBackticks(it)) {
                        registerProblem(holder, it.psi)
                    }
                }
            }
        }
    }

    private fun registerProblem(holder: ProblemsHolder, element: PsiElement) {
        holder.registerProblem(
            element,
            KotlinBundle.message("remove.redundant.backticks.quick.fix.text"),
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            RemoveRedundantBackticksQuickFix()
        )
    }
}

class RemoveRedundantBackticksQuickFix : LocalQuickFix {
    override fun getName() = KotlinBundle.message("remove.redundant.backticks.quick.fix.text")
    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        if (!isRedundantBackticks(element.node)) return
        val factory = KtPsiFactory(project)
        element.replace(factory.createIdentifier(element.text.unquote()))
    }
}

private fun isKeyword(text: String): Boolean =
    text == "yield" || text.all { it == '_' } || (KtTokens.KEYWORDS.types + KtTokens.SOFT_KEYWORDS.types).any { it.toString() == text }

private fun isRedundantBackticks(node: ASTNode): Boolean {
    val identifier = node.text
    if (!(identifier.startsWith("`") && identifier.endsWith("`"))) return false
    val unquotedText = identifier.unquote()
    if (!unquotedText.isIdentifier() || isKeyword(unquotedText)) return false
    val simpleNameStringTemplateEntry = node.psi.getStrictParentOfType<KtSimpleNameStringTemplateEntry>()
    return simpleNameStringTemplateEntry == null || canPlaceAfterSimpleNameEntry(simpleNameStringTemplateEntry.nextSibling)
}