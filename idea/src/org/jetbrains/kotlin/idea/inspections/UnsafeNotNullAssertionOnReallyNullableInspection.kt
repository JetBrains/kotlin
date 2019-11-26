/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.quickfix.ChangeCallableReturnTypeFix
import org.jetbrains.kotlin.idea.quickfix.ChangeVariableTypeFix
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.TypeUtils

class UnsafeNotNullAssertionOnReallyNullableInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return postfixExpressionVisitor(fun(expression) {
            if (expression.operationToken != KtTokens.EXCLEXCL) return
            val context = expression.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
            if (context.diagnostics.forElement(expression.operationReference)
                    .any { it.factory == Errors.UNNECESSARY_NOT_NULL_ASSERTION }
            ) return
            when (val base = expression.baseExpression) {
                is KtNameReferenceExpression -> {
                    when (val basePsi = base.mainReference.resolve()) {
//                        TODO:
//                        is KtParameter -> {
//                            val baseExpressionDescriptor = context[BindingContext.VALUE_PARAMETER, basePsi]
//                            if (baseExpressionDescriptor is ValueParameterDescriptorImpl) (baseExpressionDescriptor.returnType) else "Kek"
//                            val fix =
//                        }
                        is KtProperty -> {
                            val propertyDescriptor = context[BindingContext.REFERENCE_TARGET, base] as LocalVariableDescriptor
                            val declaration = basePsi as KtVariableDeclaration
                            val fix = ChangeVariableTypeFix.OnType(
                                declaration,
                                TypeUtils.makeNotNullable(propertyDescriptor.returnType)
                            )
                            holder.registerProblem(
                                expression, "Unsafe using of '!!' operator",
                                ProblemHighlightType.WEAK_WARNING, IntentionWrapper(fix, declaration.containingFile)
                            )
                        }
//                        else -> {
//
//                        }
                    }
                }
                is KtCallExpression -> {
                    val basePsi = base.calleeExpression?.mainReference?.resolve() ?: return
                    val fooDescriptor = context[BindingContext.FUNCTION, basePsi] ?: return
                    val fix = ChangeCallableReturnTypeFix.OnType(
                        base.calleeExpression?.mainReference?.resolve() as? KtFunction ?: return,
                        TypeUtils.makeNotNullable(fooDescriptor.returnType!!)
                    )
                    holder.registerProblem(
                        expression.operationReference, "Unsafe using of '!!' operator",
                        ProblemHighlightType.WEAK_WARNING, IntentionWrapper(fix, expression.containingFile)
                    )
                }
            }

//            is KtNameReferenceExpression -> (expression.baseExpression as KtNameReferenceExpression).mainReference.resolve() is KtParameter

//            val context = postfixExpression.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
//            val base = postfixExpression.baseExpression

//            val context = expression.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
//            val param= ((expression.baseExpression as KtNameReferenceExpression).mainReference.resolve() as KtParameter)
//            val baseExpressionDescriptor = context[BindingContext.VALUE_PARAMETER, param]
//            if(baseExpressionDescriptor is ValueParameterDescriptorImpl) (baseExpressionDescriptor.returnType) else "Kek"


//            is KtCallExpression
//            is KtNameReferenceExpression -> (expression.baseExpression as KtNameReferenceExpression).mainReference.resolve() is KtParameter

//            val context = postfixExpression.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
//            val base = postfixExpression.baseExpression

//            val context = expression.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
//            val param= ((expression.baseExpression as KtNameReferenceExpression).mainReference.resolve() as KtParameter)
//            val baseExpressionDescriptor = context[BindingContext.VALUE_PARAMETER, param]
//            if(baseExpressionDescriptor is ValueParameterDescriptorImpl) (baseExpressionDescriptor.returnType) else "Kek"
//// experiments woth local var begins
//            val myRef = postfixExpression.baseExpression as KtNameReferenceExpression
////            myRef.mainReference.resolve()
//            val propertyDescriptor = context[BindingContext.REFERENCE_TARGET, myRef] as LocalVariableDescriptor
//            val psiProperty = propertyDescriptor.source.getPsi()
//            val declaration = psiProperty as? KtProperty as KtVariableDeclaration
//            val changeValFix = ChangeVariableTypeFix.OnType(
//                declaration,
//                propertyDescriptor.returnType.constructor.declarationDescriptor?.builtIns?.stringType ?: return
//            )
//            holder.registerProblem(
//                expression, "Unsafe using of '!!' operator",
//                ProblemHighlightType.WEAK_WARNING, IntentionWrapper(changeValFix, declaration.containingFile)
//            )
//// experiments woth local var ends

//            holder.registerProblem(
//                expression,
//                "Unsafe using of '!!' operator",
//                ProblemHighlightType.WEAK_WARNING,
//                AddSaveCallAndElvisFix()
//            )
        })
    }

}

//TODO:
//private class AddSaveCallAndElvisFix : LocalQuickFix {
//    override fun getName() = "Replace unsafe assertion with safe call and elvis"
//
//    override fun getFamilyName(): String = name
//
//    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
//        applyFix(project, descriptor.psiElement as? KtPostfixExpression ?: return)
//    }
//
//    private fun applyFix(project: Project, expression: KtPostfixExpression) {
//        if (!FileModificationService.getInstance().preparePsiElementForWrite(expression)) return
////        val postfix = expression.receiverExpression as? KtPostfixExpression ?: return
//        val editor = expression.findExistingEditor()
//        expression.replaced(KtPsiFactory(expression).buildExpression {
//            appendExpression(expression.baseExpression)
//            appendFixedText("?.")
//            appendExpression(expression.selectorExpression)
//            appendFixedText("?:")
//        }).moveCaretToEnd(editor, project)
//    }
//}