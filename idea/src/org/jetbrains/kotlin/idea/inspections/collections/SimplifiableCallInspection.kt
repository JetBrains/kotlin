/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.collections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.typeUtil.builtIns

class SimplifiableCallInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        qualifiedExpressionVisitor(fun(expression) {
            val callExpression = expression.selectorExpression as? KtCallExpression ?: return
            val calleeExpression = callExpression.calleeExpression ?: return
            val (conversion, resolvedCall) = callExpression.findConversionAndResolvedCall() ?: return
            if (!conversion.callChecker(resolvedCall)) return
            val conversionSuffix = conversion.analyzer(callExpression) ?: return

            holder.registerProblem(
                calleeExpression,
                "${conversion.fqName.shortName()} call could be simplified to ${conversion.replacement}$conversionSuffix",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                SimplifyCallFix(conversion, conversionSuffix)
            )
        })

    private fun KtCallExpression.findConversionAndResolvedCall(): Pair<Conversion, ResolvedCall<*>>? {
        val calleeText = calleeExpression?.text ?: return null
        val resolvedCall: ResolvedCall<*>? by lazy { this.resolveToCall() }
        for (conversion in conversions) {
            if (conversion.shortName != calleeText) continue
            if (resolvedCall?.isCalling(conversion.fqName) == true) {
                return conversion to resolvedCall!!
            }
        }
        return null
    }

    private data class Conversion(
        val callFqName: String,
        val replacement: String,
        val analyzer: (KtCallExpression) -> String?,
        val callChecker: (ResolvedCall<*>) -> Boolean = { true }
    ) {
        val fqName = FqName(callFqName)

        val shortName = fqName.shortName().asString()
    }

    companion object {
        private fun KtCallExpression.singleLambdaExpression(): KtLambdaExpression? {
            val argument = valueArguments.singleOrNull() ?: return null
            return (argument as? KtLambdaArgument)?.getLambdaExpression() ?: argument.getArgumentExpression() as? KtLambdaExpression
        }

        private fun KtLambdaExpression.singleStatement(): KtExpression? = bodyExpression?.statements?.singleOrNull()

        private fun KtLambdaExpression.singleLambdaParameterName(): String? {
            val lambdaParameters = valueParameters
            return if (lambdaParameters.isNotEmpty()) lambdaParameters.singleOrNull()?.name else "it"
        }

        private fun KtExpression.isNameReferenceTo(name: String): Boolean =
            this is KtNameReferenceExpression && this.getReferencedName() == name

        private fun KtExpression.isNull(): Boolean =
            this is KtConstantExpression && this.node.elementType == KtNodeTypes.NULL

        private val conversions = listOf(
            Conversion("kotlin.collections.flatMap", "flatten", fun(callExpression: KtCallExpression): String? {
                val lambdaExpression = callExpression.singleLambdaExpression() ?: return null
                val reference = lambdaExpression.singleStatement() ?: return null
                val lambdaParameterName = lambdaExpression.singleLambdaParameterName() ?: return null
                if (!reference.isNameReferenceTo(lambdaParameterName)) return null
                return "()"
            }),

            Conversion("kotlin.collections.filter", "filterNotNull", analyzer = fun(callExpression: KtCallExpression): String? {
                val lambdaExpression = callExpression.singleLambdaExpression() ?: return null
                val statement = lambdaExpression.singleStatement() as? KtBinaryExpression ?: return null
                val lambdaParameterName = lambdaExpression.singleLambdaParameterName() ?: return null
                if (statement.operationToken != KtTokens.EXCLEQ && statement.operationToken != KtTokens.EXCLEQEQEQ) return null
                val left = statement.left ?: return null
                val right = statement.right ?: return null
                if (left.isNameReferenceTo(lambdaParameterName) && right.isNull()) {
                    return "()"
                } else if (right.isNameReferenceTo(lambdaParameterName) && left.isNull()) {
                    return "()"
                }
                return null
            }, callChecker = fun(resolvedCall: ResolvedCall<*>): Boolean {
                val extensionReceiverType = resolvedCall.extensionReceiver?.type ?: return false
                return extensionReceiverType.constructor.declarationDescriptor?.defaultType?.isMap(extensionReceiverType.builtIns) == false
            })
        )
    }

    private class SimplifyCallFix(val conversion: Conversion, val conversionSuffix: String) : LocalQuickFix {
        override fun getName() = "Convert '${conversion.fqName.shortName()}' call to '${conversion.replacement}$conversionSuffix'"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val callExpression = descriptor.psiElement.parent as? KtCallExpression ?: return
            callExpression.replace(KtPsiFactory(callExpression).createExpression("${conversion.replacement}$conversionSuffix"))
        }
    }
}

