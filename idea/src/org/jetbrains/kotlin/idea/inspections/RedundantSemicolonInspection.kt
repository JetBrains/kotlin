/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.util.isLineBreak
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class RedundantSemicolonInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)

                if (element.node.elementType == KtTokens.SEMICOLON && isRedundantSemicolon(element)) {
                    holder.registerProblem(
                        element,
                        KotlinBundle.message("redundant.semicolon"),
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        Fix
                    )
                }
            }
        }
    }

    companion object {
        fun isRedundantSemicolon(semicolon: PsiElement): Boolean {
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

            (semicolon.parent.parent as? KtClass)?.let { clazz ->
                if (clazz.isEnum() && clazz.getChildrenOfType<KtEnumEntry>().isEmpty()) {
                    if (semicolon.prevLeaf {
                            it !is PsiWhiteSpace && it !is PsiComment && !it.isLineBreak()
                        }?.node?.elementType == KtTokens.LBRACE &&
                        clazz.declarations.isNotEmpty()
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

            semicolon.prevLeaf()?.parent?.safeAs<KtIfExpression>()?.also { ifExpression ->
                if (ifExpression.then == null)
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

            val prevSibling = semicolon.getPrevSiblingIgnoringWhitespaceAndComments()
            val nextSibling = semicolon.getNextSiblingIgnoringWhitespaceAndComments()
            if (prevSibling.safeAs<KtNameReferenceExpression>()?.text in softModifierKeywords &&
                nextSibling is KtDeclaration
            ) return false
            if (nextSibling.safeAs<KtPrefixExpression>()?.operationToken == KtTokens.EXCL &&
                semicolon.prevLeaf()?.getStrictParentOfType<KtTypeReference>() != null
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

        private object Fix : LocalQuickFix {
            override fun getName() = KotlinBundle.message("fix.text")
            override fun getFamilyName() = name

            override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                if (!FileModificationService.getInstance().preparePsiElementForWrite(descriptor.psiElement)) return
                descriptor.psiElement.delete()
            }
        }

        private val softModifierKeywords = KtTokens.SOFT_KEYWORDS.types.mapNotNull { (it as? KtModifierKeywordToken)?.toString() }
    }
}
