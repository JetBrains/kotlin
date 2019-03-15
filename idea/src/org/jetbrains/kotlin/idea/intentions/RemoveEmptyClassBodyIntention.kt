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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInspection.CleanupLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.editor.fixers.range
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class RemoveEmptyClassBodyInspection :
        IntentionBasedInspection<KtClassBody>(RemoveEmptyClassBodyIntention::class), CleanupLocalInspectionTool {
    override fun problemHighlightType(element: KtClassBody): ProblemHighlightType =
            ProblemHighlightType.LIKE_UNUSED_SYMBOL
}

class RemoveEmptyClassBodyIntention : SelfTargetingOffsetIndependentIntention<KtClassBody>(KtClassBody::class.java, "Redundant empty class body") {

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