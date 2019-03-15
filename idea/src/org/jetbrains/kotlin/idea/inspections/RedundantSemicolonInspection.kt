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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

class RedundantSemicolonInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)

                if (element.node.elementType == KtTokens.SEMICOLON && isRedundant(element)) {
                    holder.registerProblem(
                        element,
                        "Redundant semicolon",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        Fix
                    )
                }
            }
        }
    }

    private fun isRedundant(semicolon: PsiElement): Boolean {
        val nextLeaf = semicolon.nextLeaf { it !is PsiWhiteSpace && it !is PsiComment || it.isLineBreak() }
        val isAtEndOfLine = nextLeaf == null || nextLeaf.isLineBreak()
        if (!isAtEndOfLine) {
            //when there is no imports parser generates empty import list with no spaces
            if (semicolon.parent is KtPackageDirective && (nextLeaf as? KtImportList)?.imports?.isEmpty() == true) {
                return true
            }
            return false
        }

        if (semicolon.prevLeaf()?.node?.elementType == KtNodeTypes.ELSE) return false

        if (semicolon.parent is KtEnumEntry) return false

        (semicolon.parent.parent as? KtClass)?.let {
            if (it.isEnum() && it.getChildrenOfType<KtEnumEntry>().isEmpty()) {
                if (semicolon.prevLeaf { it !is PsiWhiteSpace && it !is PsiComment && !it.isLineBreak() }?.node?.elementType == KtTokens.LBRACE
                    && it.declarations.isNotEmpty()
                ) {
                    //first semicolon in enum with no entries, but with some declarations
                    return false
                }
            }
        }

        (semicolon.prevLeaf()?.parent as? KtLoopExpression)?.let {
            if (it !is KtDoWhileExpression && it.body == null)
                return false
        }

        if (nextLeaf?.nextLeaf {
                it !is PsiWhiteSpace && it !is PsiComment && it.getStrictParentOfType<KDoc>() == null &&
                        it.getStrictParentOfType<KtAnnotationEntry>() == null
            }?.node?.elementType == KtTokens.LBRACE) {
            return false // case with statement starting with '{' and call on the previous line
        }

        if (isRequiredForCompanion(semicolon)) {
            return false
        }

        val prevNameReference = semicolon.getPrevSiblingIgnoringWhitespaceAndComments() as? KtNameReferenceExpression
        if (prevNameReference != null && prevNameReference.text in softModifierKeywords
            && semicolon.getNextSiblingIgnoringWhitespaceAndComments() is KtDeclaration
        ) return false

        return true
    }

    private fun isRequiredForCompanion(semicolon: PsiElement): Boolean {
        val prev = semicolon.getPrevSiblingIgnoringWhitespaceAndComments() as? KtObjectDeclaration ?: return false
        if (!prev.isCompanion()) return false
        if (prev.nameIdentifier != null || prev.getChildOfType<KtClassBody>() != null) return false

        val next = semicolon.getNextSiblingIgnoringWhitespaceAndComments() ?: return false
        val firstChildNode = next.firstChild?.node ?: return false
        if (KtTokens.KEYWORDS.contains(firstChildNode.elementType)) return false

        return true
    }

    private fun PsiElement?.isLineBreak() = this is PsiWhiteSpace && textContains('\n')

    private object Fix : LocalQuickFix {
        override fun getName() = "Remove redundant semicolon"
        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            if (!FileModificationService.getInstance().preparePsiElementForWrite(descriptor.psiElement)) return
            descriptor.psiElement.delete()
        }
    }

    private val softModifierKeywords = KtTokens.SOFT_KEYWORDS.types.mapNotNull { (it as? KtModifierKeywordToken)?.toString() }
}
