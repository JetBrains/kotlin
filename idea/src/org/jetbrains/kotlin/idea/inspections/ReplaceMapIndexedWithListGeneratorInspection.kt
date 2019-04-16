/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.collections.isCalling
import org.jetbrains.kotlin.idea.inspections.collections.isCollection
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.hasUsages
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getImplicitReceiverValue
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ReplaceMapIndexedWithListGeneratorInspection : AbstractKotlinInspection() {
    private companion object {
        private const val MAP_INDEXED_FUNCTION_NAME = "mapIndexed"
        private val MAP_INDEXED_FQ_NAME = FqName("kotlin.collections.mapIndexed")
    }
    
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = callExpressionVisitor(fun(callExpression) {
        val callee = callExpression.calleeExpression ?: return
        if (callee.text != MAP_INDEXED_FUNCTION_NAME
            && (callee.mainReference?.resolve() as? KtNamedFunction)?.name != MAP_INDEXED_FUNCTION_NAME
        ) return
        val context = callExpression.analyze(BodyResolveMode.PARTIAL)
        val resolvedCall = callExpression.getResolvedCall(context) ?: return
        if (!resolvedCall.isCalling(MAP_INDEXED_FQ_NAME)) return
        val receiver = callExpression.getQualifiedExpressionForSelector()?.receiverExpression
        val receiverType = receiver?.getResolvedCall(context)?.resultingDescriptor?.returnType
            ?: resolvedCall.getImplicitReceiverValue()?.type ?: return
        if (!receiverType.isCollection(DefaultBuiltIns.Instance)) return
        val valueArgument = callExpression.valueArguments.singleOrNull() ?: callExpression.lambdaArguments.singleOrNull() ?: return
        val valueParameters = when (val argumentExpression = valueArgument.getLambdaOrNamedFunction()) {
            is KtLambdaExpression -> argumentExpression.valueParameters
            is KtNamedFunction -> argumentExpression.valueParameters
            else -> return
        }
        if (valueParameters.size != 2) return
        val secondParameter = valueParameters[1]
        val destructuringDeclaration = secondParameter.destructuringDeclaration
        if (destructuringDeclaration != null) {
            if (destructuringDeclaration.entries.any { entry -> entry.hasUsages(callExpression) }) return
        } else if (secondParameter.hasUsages(callExpression)) {
            return
        }
        holder.registerProblem(callee, KotlinBundle.message("should.be.replaced.with.list.generator"), ReplaceWithListGeneratorFix())
    })

    private class ReplaceWithListGeneratorFix : LocalQuickFix {
        override fun getName() = KotlinBundle.message("replace.with.list.generator.fix.text")
        override fun getFamilyName() = name
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val callExpression = descriptor.psiElement.getStrictParentOfType<KtCallExpression>() ?: return
            val qualifiedExpression = callExpression.getQualifiedExpressionForSelector()
            val receiverExpression = qualifiedExpression?.receiverExpression
            val valueArgument = callExpression.valueArguments.singleOrNull() ?: callExpression.lambdaArguments.singleOrNull() ?: return
            val psiFactory = KtPsiFactory(callExpression)
            when (val argumentExpression = valueArgument.getLambdaOrNamedFunction()) {
                is KtLambdaExpression -> {
                    val functionLiteral = argumentExpression.functionLiteral
                    functionLiteral.valueParameterList?.replace(
                        psiFactory.createLambdaParameterList(argumentExpression.valueParameters.first().text)
                    )
                    functionLiteral.collectDescendantsOfType<KtReturnExpression>().forEach {
                        val returnedExpression = it.returnedExpression ?: return@forEach
                        if (it.analyze()[BindingContext.LABEL_TARGET, it.getTargetLabel()] == functionLiteral) {
                            it.replace(psiFactory.createExpressionByPattern("return@List $0", returnedExpression))
                        }
                    }
                    if (receiverExpression != null) {
                        qualifiedExpression.replace(
                            psiFactory.createExpressionByPattern("List($0.size) $1", receiverExpression, argumentExpression.text)
                        )
                    } else {
                        callExpression.replace(psiFactory.createExpressionByPattern("List(size) ${argumentExpression.text}"))
                    }
                }
                is KtNamedFunction -> {
                    argumentExpression.valueParameterList?.replace(
                        psiFactory.createParameterList("(${argumentExpression.valueParameters.first().text})")
                    )
                    if (receiverExpression != null) {
                        qualifiedExpression.replace(
                            psiFactory.createExpressionByPattern("List($0.size, $1)", receiverExpression, argumentExpression.text)
                        )
                    } else {
                        callExpression.replace(psiFactory.createExpressionByPattern("List(size, ${argumentExpression.text})"))
                    }
                }
                else -> return
            }
        }
    }
}

private fun KtValueArgument.getLambdaOrNamedFunction(): KtExpression? {
    return when (val argumentExpression = getArgumentExpression()) {
        is KtLambdaExpression, is KtNamedFunction -> argumentExpression
        is KtLabeledExpression -> argumentExpression.baseExpression as? KtLambdaExpression
        else -> null
    }
}