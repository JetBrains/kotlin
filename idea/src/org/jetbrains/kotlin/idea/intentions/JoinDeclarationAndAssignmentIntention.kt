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
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.canOmitDeclaredType
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.core.unblockDocument
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

class JoinDeclarationAndAssignmentInspection : IntentionBasedInspection<KtProperty>(
        JoinDeclarationAndAssignmentIntention::class,
        "Can be joined with assignment"
)

class JoinDeclarationAndAssignmentIntention : SelfTargetingOffsetIndependentIntention<KtProperty>(
        KtProperty::class.java,
        "Join declaration and assignment"
) {

    private fun equalNullableTypes(type1: KotlinType?, type2: KotlinType?): Boolean {
        if (type1 == null) return type2 == null
        if (type2 == null) return false
        return TypeUtils.equalTypes(type1, type2)
    }

    override fun isApplicableTo(element: KtProperty): Boolean {
        if (element.hasDelegate()
            || element.hasInitializer()
            || element.setter != null
            || element.getter != null
            || element.receiverTypeReference != null
            || element.name == null) {
            return false
        }

        val assignment = findAssignment(element) ?: return false
        return assignment.right?.let {
            hasNoLocalDependencies(it, element.parent) &&
            assignment.analyze().let { context ->
                (element.isVar && !element.isLocal) ||
                equalNullableTypes(it.getType(context), context[BindingContext.TYPE, element.typeReference])
            }
        } ?: false
    }

    override fun applyTo(element: KtProperty, editor: Editor?) {
        val typeReference = element.typeReference ?: return

        val assignment = findAssignment(element) ?: return
        val initializer = assignment.right ?: return
        val newInitializer = element.setInitializer(initializer)!!

        val initializerBlock = assignment.parent.parent as? KtAnonymousInitializer
        assignment.delete()
        if (initializerBlock != null && (initializerBlock.body as? KtBlockExpression)?.isEmpty() == true) {
            initializerBlock.delete()
        }

        editor?.apply {
            unblockDocument()

            if (element.canOmitDeclaredType(newInitializer, canChangeTypeToSubtype = !element.isVar)) {
                val colon = element.colon!!
                selectionModel.setSelection(colon.startOffset, typeReference.endOffset)
                moveCaret(typeReference.endOffset, ScrollType.CENTER)
            }
            else {
                moveCaret(newInitializer.startOffset, ScrollType.CENTER)
            }
        }
    }

    private fun findAssignment(property: KtProperty): KtBinaryExpression? {
        val propertyContainer = property.parent as? KtElement ?: return null
        property.typeReference ?: return null

        val assignments = mutableListOf<KtBinaryExpression>()
        fun process(binaryExpr: KtBinaryExpression) {
            if (binaryExpr.operationToken != KtTokens.EQ) return
            val left = binaryExpr.left
            val leftReference = when (left) {
                is KtNameReferenceExpression ->
                    left
                is KtDotQualifiedExpression ->
                    if (left.receiverExpression is KtThisExpression) left.selectorExpression as? KtNameReferenceExpression else null
                else ->
                    null
            } ?: return
            if (leftReference.getReferencedName() != property.name) return
            assignments += binaryExpr
        }
        propertyContainer.forEachDescendantOfType(::process)

        fun PsiElement?.invalidParent(): Boolean {
            when {
                this == null -> return true
                this === propertyContainer -> return false
                else -> {
                    val grandParent = parent
                    if (grandParent.parent !== propertyContainer) return true
                    return grandParent !is KtAnonymousInitializer && grandParent !is KtSecondaryConstructor
                }
            }
        }

        if (assignments.any { it.parent.invalidParent() }) return null

        val first = assignments.firstOrNull() ?: return null
        if (assignments.any { it !== first && it.parent.parent is KtSecondaryConstructor}) return null

        if (propertyContainer !is KtClassBody) return first

        val blockParent = first.parent as? KtBlockExpression ?: return null
        return if (blockParent.statements.firstOrNull() == first) first else null
    }

    // a block that only contains comments is not empty
    private fun KtBlockExpression.isEmpty() = contentRange().isEmpty

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
