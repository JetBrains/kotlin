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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceService
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.core.canOmitDeclaredType
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.core.unblockDocument
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class JoinDeclarationAndAssignmentIntention :
        SelfTargetingIntention<KtBinaryExpression>(KtBinaryExpression::class.java, "Join declaration and assignment") {

    override fun isApplicableTo(element: KtBinaryExpression, caretOffset: Int): Boolean {
        if (element.operationToken != KtTokens.EQ) {
            return false
        }
        val rightExpression = element.right ?: return false

        val initializer = PsiTreeUtil.getParentOfType(element,
                                                      KtAnonymousInitializer::class.java,
                                                      KtSecondaryConstructor::class.java) ?: return false

        val target = findTargetProperty(element)
        return target != null && target.initializer == null && target.receiverTypeReference == null &&
               target.getNonStrictParentOfType<KtClassOrObject>() == element.getNonStrictParentOfType<KtClassOrObject>() &&
               hasNoLocalDependencies(rightExpression, initializer)

    }

    override fun applyTo(element: KtBinaryExpression, editor: Editor?) {
        val property = findTargetProperty(element) ?: return
        val initializer = element.right ?: return
        val newInitializer = property.setInitializer(initializer)!!

        val initializerBlock = element.getStrictParentOfType<KtAnonymousInitializer>()
        element.delete()
        if (initializerBlock != null && (initializerBlock.body as? KtBlockExpression)?.isEmpty() == true) {
            initializerBlock.delete()
        }

        editor?.apply {
            unblockDocument()

            val typeRef = property.typeReference
            if (typeRef != null && property.canOmitDeclaredType(newInitializer, canChangeTypeToSubtype = !property.isVar)) {
                val colon = property.colon!!
                selectionModel.setSelection(colon.startOffset, typeRef.endOffset)
                moveCaret(typeRef.endOffset, ScrollType.CENTER)
            }
            else {
                moveCaret(newInitializer.startOffset, ScrollType.CENTER)
            }
        }
    }

    private fun findTargetProperty(expr: KtBinaryExpression): KtProperty? {
        val leftExpression = expr.left as? KtNameReferenceExpression ?: return null
        return leftExpression.resolveAllReferences().firstIsInstanceOrNull<KtProperty>()
    }

    fun KtBlockExpression.isEmpty(): Boolean {
        // a block that only contains comments is not empty
        return contentRange().isEmpty
    }

    private fun hasNoLocalDependencies(element: KtElement, localContext: PsiElement): Boolean {
        return !element.anyDescendantOfType<PsiElement> { child ->
            child.resolveAllReferences().any { it != null && PsiTreeUtil.isAncestor(localContext, it, false) }
        }
    }
}

private fun PsiElement.resolveAllReferences(): Sequence<PsiElement?> {
    return PsiReferenceService.getService().getReferences(this, PsiReferenceService.Hints.NO_HINTS)
            .asSequence()
            .map { it.resolve() }
}


