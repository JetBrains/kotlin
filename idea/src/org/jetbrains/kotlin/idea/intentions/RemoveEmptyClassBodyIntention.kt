/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInspection.CleanupLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

@Suppress("DEPRECATION")
class RemoveEmptyClassBodyInspection :
    IntentionBasedInspection<KtClassBody>(RemoveEmptyClassBodyIntention::class), CleanupLocalInspectionTool {
    override fun problemHighlightType(element: KtClassBody): ProblemHighlightType =
        ProblemHighlightType.LIKE_UNUSED_SYMBOL
}

class RemoveEmptyClassBodyIntention :
    SelfTargetingOffsetIndependentIntention<KtClassBody>(KtClassBody::class.java, "Redundant empty class body") {

    override fun applyTo(element: KtClassBody, editor: Editor?) {
        val parent = element.parent
        element.delete()
        addSemicolonAfterEmptyCompanion(parent, editor)
    }

    private fun addSemicolonAfterEmptyCompanion(element: PsiElement, editor: Editor?) {
        if (element !is KtObjectDeclaration) return
        if (!element.isCompanion() || element.nameIdentifier != null) return

        val next = element.getNextSiblingIgnoringWhitespaceAndComments() ?: return
        if (next.node.elementType == KtTokens.SEMICOLON) return
        val firstChildNode = next.firstChild?.node ?: return
        if (firstChildNode.elementType in KtTokens.KEYWORDS) return

        element.parent.addAfter(KtPsiFactory(element).createSemicolon(), element)
        editor?.caretModel?.moveToOffset(element.endOffset + 1)
    }

    override fun isApplicableTo(element: KtClassBody): Boolean {
        element.getStrictParentOfType<KtObjectDeclaration>()?.let {
            if (it.isObjectLiteral()) return false
        }

        element.getStrictParentOfType<KtClass>()?.let {
            if (!it.isTopLevel() && it.getNextSiblingIgnoringWhitespaceAndComments() is KtSecondaryConstructor) return false
        }

        return element.text.replace("{", "").replace("}", "").isBlank()
    }
}