/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.coroutines

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.core.setType
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getTargetFunctionDescriptor

class AddCallOrUnwrapTypeFix(
    val withBody: Boolean,
    val functionName: String,
    val typeName: String,
    val shouldMakeSuspend: Boolean,
    val simplify: (KtExpression) -> Unit
) : LocalQuickFix {
    override fun getName(): String =
        if (withBody) "Add '.$functionName()' to function result (breaks use-sites!)"
        else "Unwrap '$typeName' return type (breaks use-sites!)"

    override fun getFamilyName(): String = name

    private fun KtExpression.addCallAndSimplify(factory: KtPsiFactory) {
        val newCallExpression = factory.createExpressionByPattern("$0.$functionName()", this)
        val result = replaced(newCallExpression)
        simplify(result)
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val function = descriptor.psiElement.getNonStrictParentOfType<KtFunction>() ?: return
        val returnTypeReference = function.getReturnTypeReference()
        val context = function.analyzeWithContent()
        val functionDescriptor = context[BindingContext.FUNCTION, function] ?: return
        if (shouldMakeSuspend) {
            function.addModifier(KtTokens.SUSPEND_KEYWORD)
        }
        if (returnTypeReference != null) {
            val returnType = functionDescriptor.returnType ?: return
            val returnTypeArgument = returnType.arguments.firstOrNull()?.type ?: return
            function.setType(returnTypeArgument)
        }
        if (!withBody) return
        val factory = KtPsiFactory(project)
        val bodyExpression = function.bodyExpression
        bodyExpression?.forEachDescendantOfType<KtReturnExpression> {
            if (it.getTargetFunctionDescriptor(context) == functionDescriptor) {
                it.returnedExpression?.addCallAndSimplify(factory)
            }
        }
        if (function is KtFunctionLiteral) {
            val lastStatement = function.bodyExpression?.statements?.lastOrNull()
            if (lastStatement != null && lastStatement !is KtReturnExpression) {
                lastStatement.addCallAndSimplify(factory)
            }
        } else if (!function.hasBlockBody()) {
            bodyExpression?.addCallAndSimplify(factory)
        }
    }
}