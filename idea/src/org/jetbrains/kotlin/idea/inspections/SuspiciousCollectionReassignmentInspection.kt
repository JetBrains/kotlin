/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.intentions.ReplaceWithOrdinaryAssignmentIntention
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.quickfix.ChangeToMutableCollectionFix
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType

class SuspiciousCollectionReassignmentInspection : AbstractKotlinInspection() {

    private val targetOperations: List<KtSingleValueToken> = listOf(KtTokens.PLUSEQ, KtTokens.MINUSEQ)

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        binaryExpressionVisitor(fun(binaryExpression) {
            if (binaryExpression.right == null) return
            val operationToken = binaryExpression.operationToken as? KtSingleValueToken ?: return
            if (operationToken !in targetOperations) return
            val left = binaryExpression.left ?: return
            val property = left.mainReference?.resolve() as? KtProperty ?: return
            if (!property.isVar) return

            val context = binaryExpression.analyze()
            val leftType = left.getType(context) ?: return
            val leftDefaultType = leftType.constructor.declarationDescriptor?.defaultType ?: return
            if (!leftType.isReadOnlyCollectionOrMap(binaryExpression.builtIns)) return
            if (context.diagnostics.forElement(binaryExpression).any { it.severity == Severity.ERROR }) return

            val fixes = mutableListOf<LocalQuickFix>()
            if (ChangeTypeToMutableFix.isApplicable(property)) {
                fixes.add(ChangeTypeToMutableFix(leftType))
            }
            if (ReplaceWithFilterFix.isApplicable(binaryExpression, leftDefaultType, context)) {
                fixes.add(ReplaceWithFilterFix())
            }
            when {
                ReplaceWithAssignmentFix.isApplicable(binaryExpression, property, context) -> fixes.add(ReplaceWithAssignmentFix())
                JoinWithInitializerFix.isApplicable(binaryExpression, property) -> fixes.add(JoinWithInitializerFix(operationToken))
                else -> fixes.add(IntentionWrapper(ReplaceWithOrdinaryAssignmentIntention(), binaryExpression.containingKtFile))
            }

            val typeText = leftDefaultType.toString().takeWhile { it != '<' }.toLowerCase()
            val operationReference = binaryExpression.operationReference
            holder.registerProblem(
                operationReference,
                "'${operationReference.text}' create new $typeText under the hood",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                *fixes.toTypedArray()
            )
        })

    private class ChangeTypeToMutableFix(private val type: KotlinType) : LocalQuickFix {
        override fun getName() = "Change type to mutable"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val operationReference = descriptor.psiElement as? KtOperationReferenceExpression ?: return
            val binaryExpression = operationReference.parent as? KtBinaryExpression ?: return
            val left = binaryExpression.left ?: return
            val property = left.mainReference?.resolve() as? KtProperty ?: return
            ChangeToMutableCollectionFix.applyFix(property, type)
            property.valOrVarKeyword.replace(KtPsiFactory(property).createValKeyword())
            binaryExpression.findExistingEditor()?.caretModel?.moveToOffset(property.endOffset)
        }

        companion object {
            fun isApplicable(property: KtProperty): Boolean {
                return ChangeToMutableCollectionFix.isApplicable(property)
            }
        }
    }

    private class ReplaceWithFilterFix : LocalQuickFix {
        override fun getName() = "Replace with filter"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val operationReference = descriptor.psiElement as? KtOperationReferenceExpression ?: return
            val binaryExpression = operationReference.parent as? KtBinaryExpression ?: return
            val left = binaryExpression.left ?: return
            val right = binaryExpression.right ?: return
            val psiFactory = KtPsiFactory(operationReference)
            operationReference.replace(psiFactory.createOperationName(KtTokens.EQ.value))
            right.replace(psiFactory.createExpressionByPattern("$0.filter { it !in $1 }", left, right))
        }

        companion object {
            fun isApplicable(binaryExpression: KtBinaryExpression, leftType: SimpleType, context: BindingContext): Boolean {
                if (binaryExpression.operationToken != KtTokens.MINUSEQ) return false
                if (leftType == binaryExpression.builtIns.map.defaultType) return false
                return binaryExpression.right?.getType(context)?.classDescriptor()?.isSubclassOf(binaryExpression.builtIns.iterable) == true
            }
        }
    }

    private class ReplaceWithAssignmentFix : LocalQuickFix {
        override fun getName() = "Replace with assignment (original is empty)"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val operationReference = descriptor.psiElement as? KtOperationReferenceExpression ?: return
            val psiFactory = KtPsiFactory(operationReference)
            operationReference.replace(psiFactory.createOperationName(KtTokens.EQ.value))
        }

        companion object {
            val emptyCollectionFactoryMethods =
                listOf("emptyList", "emptySet", "emptyMap", "listOf", "setOf", "mapOf").map { "kotlin.collections.$it" }

            fun isApplicable(binaryExpression: KtBinaryExpression, property: KtProperty, context: BindingContext): Boolean {
                if (binaryExpression.operationToken != KtTokens.PLUSEQ) return false

                if (!property.isLocal) return false
                val initializer = property.initializer as? KtCallExpression ?: return false

                if (initializer.valueArguments.isNotEmpty()) return false
                val initializerResultingDescriptor = initializer.getResolvedCall(context)?.resultingDescriptor
                val fqName = initializerResultingDescriptor?.fqNameOrNull()?.asString()
                if (fqName !in emptyCollectionFactoryMethods) return false

                val rightClassDescriptor = binaryExpression.right?.getType(context)?.classDescriptor() ?: return false
                val initializerClassDescriptor = initializerResultingDescriptor?.returnType?.classDescriptor() ?: return false
                if (!rightClassDescriptor.isSubclassOf(initializerClassDescriptor)) return false

                if (binaryExpression.siblings(forward = false, withItself = false)
                        .filter { it != property }
                        .any { sibling -> sibling.anyDescendantOfType<KtSimpleNameExpression> { it.mainReference.resolve() == property } }
                ) return false

                return true
            }
        }
    }

    private class JoinWithInitializerFix(private val op: KtSingleValueToken) : LocalQuickFix {
        override fun getName() = "Join with initializer"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val operationReference = descriptor.psiElement as? KtOperationReferenceExpression ?: return
            val binaryExpression = operationReference.parent as? KtBinaryExpression ?: return
            val left = binaryExpression.left ?: return
            val right = binaryExpression.right ?: return
            val property = left.mainReference?.resolve() as? KtProperty ?: return
            val initializer = property.initializer ?: return

            val psiFactory = KtPsiFactory(operationReference)
            val newOp = if (op == KtTokens.PLUSEQ) KtTokens.PLUS else KtTokens.MINUS
            val replaced = initializer.replaced(psiFactory.createExpressionByPattern("$0 $1 $2", initializer, newOp.value, right))
            binaryExpression.delete()
            property.findExistingEditor()?.caretModel?.moveToOffset(replaced.endOffset)
        }

        companion object {
            fun isApplicable(binaryExpression: KtBinaryExpression, property: KtProperty): Boolean {
                if (!property.isLocal || property.initializer == null) return false
                return binaryExpression.getPrevSiblingIgnoringWhitespaceAndComments() == property
            }
        }
    }
}

private fun KotlinType.classDescriptor() = constructor.declarationDescriptor as? ClassDescriptor

internal fun KotlinType.isReadOnlyCollectionOrMap(builtIns: KotlinBuiltIns): Boolean {
    val leftDefaultType = constructor.declarationDescriptor?.defaultType ?: return false
    return leftDefaultType in listOf(builtIns.list.defaultType, builtIns.set.defaultType, builtIns.map.defaultType)
}