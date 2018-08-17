/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinIntroduceVariableHandler
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType

class SuspiciousCollectionReassignmentInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        binaryExpressionVisitor(fun(binaryExpression) {
            if (binaryExpression.right == null) return
            if (binaryExpression.operationToken !in listOf(KtTokens.PLUSEQ, KtTokens.MINUSEQ)) return
            val left = binaryExpression.left ?: return
            val property = left.mainReference?.resolve() as? KtProperty ?: return
            if (!property.isVar) return

            val context = binaryExpression.analyze(BodyResolveMode.PARTIAL)
            val type = left.getType(context) ?: return
            val defaultType = type.constructor.declarationDescriptor?.defaultType ?: return
            val builtIns = binaryExpression.builtIns
            if (defaultType !in listOf(builtIns.list.defaultType, builtIns.set.defaultType, builtIns.map.defaultType)) return

            val fixes = mutableListOf<LocalQuickFix>(AssignToLocalVariableFix())
            if (property.initializer != null && property.isLocal) fixes.add(ChangeTypeToMutable(type))
            val typeText = defaultType.toString().takeWhile { it != '<' }.toLowerCase()
            holder.registerProblem(
                binaryExpression,
                "'${binaryExpression.operationReference.text}' create new $typeText under the hood",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                *fixes.toTypedArray()
            )
        })

    private class AssignToLocalVariableFix : LocalQuickFix {
        override fun getName() = "Assign to local variable"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val binaryExpression = descriptor.psiElement as? KtBinaryExpression ?: return
            val left = binaryExpression.left ?: return
            val right = binaryExpression.right ?: return
            val newOperation = when (binaryExpression.operationToken) {
                KtTokens.PLUSEQ -> KtTokens.PLUS.value
                KtTokens.MINUSEQ -> KtTokens.MINUS.value
                else -> return
            }
            val editor = binaryExpression.findExistingEditor() ?: return
            val replaced = binaryExpression.replaced(
                KtPsiFactory(binaryExpression).createExpressionByPattern("$0 $1 $2", left, newOperation, right)
            )
            KotlinIntroduceVariableHandler.doRefactoring(
                binaryExpression.project, editor, replaced, isVar = false, occurrencesToReplace = null, onNonInteractiveFinish = null
            )
        }
    }

    private class ChangeTypeToMutable(private val type: KotlinType) : LocalQuickFix {
        override fun getName() = "Change type to mutable"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val binaryExpression = descriptor.psiElement as? KtBinaryExpression ?: return
            val left = binaryExpression.left ?: return
            val property = left.mainReference?.resolve() as? KtProperty ?: return
            val initializer = property.initializer ?: return
            val fqName = initializer.resolveToCall()?.resultingDescriptor?.fqNameOrNull()?.asString()
            val psiFactory = KtPsiFactory(binaryExpression)
            val mutableOf = when (fqName) {
                "kotlin.collections.listOf" -> "mutableListOf"
                "kotlin.collections.setOf" -> "mutableSetOf"
                "kotlin.collections.mapOf" -> "mutableMapOf"
                else -> null
            }
            if (mutableOf != null) {
                (initializer as? KtCallExpression)?.calleeExpression?.replaced(psiFactory.createExpression(mutableOf)) ?: return
            } else {
                val builtIns = binaryExpression.builtIns
                val toMutable = when (type.constructor) {
                    builtIns.list.defaultType.constructor -> "toMutableList"
                    builtIns.set.defaultType.constructor -> "toMutableSet"
                    builtIns.map.defaultType.constructor -> "toMutableMap"
                    else -> null
                } ?: return
                val dotQualifiedExpression = initializer.replaced(
                    psiFactory.createExpressionByPattern("($0).$1()", initializer, toMutable)
                ) as KtDotQualifiedExpression
                val receiver = dotQualifiedExpression.receiverExpression
                val deparenthesize = KtPsiUtil.deparenthesize(dotQualifiedExpression.receiverExpression)
                if (deparenthesize != null && receiver != deparenthesize) receiver.replace(deparenthesize)
            }
            property.typeReference?.also { it.replace(psiFactory.createType("Mutable${it.text}")) }
            property.valOrVarKeyword.replace(psiFactory.createValKeyword())
            binaryExpression.findExistingEditor()?.caretModel?.moveToOffset(property.endOffset)
        }
    }
}