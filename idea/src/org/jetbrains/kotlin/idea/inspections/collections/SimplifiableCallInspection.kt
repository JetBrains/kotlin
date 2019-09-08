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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.debugger.sequence.psi.receiverType
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class SimplifiableCallInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        callExpressionVisitor(fun(callExpression) {
            val calleeExpression = callExpression.calleeExpression ?: return
            val (conversion, resolvedCall) = callExpression.findConversionAndResolvedCall() ?: return
            if (!conversion.callChecker(resolvedCall)) return
            val replacement = conversion.analyzer(callExpression) ?: return

            holder.registerProblem(
                calleeExpression,
                "${conversion.shortName} call could be simplified to $replacement",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                SimplifyCallFix(conversion, replacement)
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
            Conversion("kotlin.collections.flatMap", fun(callExpression: KtCallExpression): String? {
                val lambdaExpression = callExpression.singleLambdaExpression() ?: return null
                val reference = lambdaExpression.singleStatement() ?: return null
                val lambdaParameterName = lambdaExpression.singleLambdaParameterName() ?: return null
                if (!reference.isNameReferenceTo(lambdaParameterName)) return null
                val receiverType = callExpression.receiverType() ?: return null
                if (KotlinBuiltIns.isPrimitiveArray(receiverType)) return null
                if (KotlinBuiltIns.isArray(receiverType)
                    && receiverType.arguments.firstOrNull()?.type?.let { KotlinBuiltIns.isArray(it) } != true
                ) return null
                return "flatten()"
            }),

            Conversion("kotlin.collections.filter", analyzer = fun(callExpression: KtCallExpression): String? {
                val lambdaExpression = callExpression.singleLambdaExpression() ?: return null
                val lambdaParameterName = lambdaExpression.singleLambdaParameterName() ?: return null
                when (val statement = lambdaExpression.singleStatement() ?: return null) {
                    is KtBinaryExpression -> {
                        if (statement.operationToken != KtTokens.EXCLEQ && statement.operationToken != KtTokens.EXCLEQEQEQ) return null
                        val left = statement.left ?: return null
                        val right = statement.right ?: return null
                        if (left.isNameReferenceTo(lambdaParameterName) && right.isNull() ||
                            right.isNameReferenceTo(lambdaParameterName) && left.isNull()
                        ) {
                            return "filterNotNull()"
                        }
                    }
                    is KtIsExpression -> {
                        if (statement.isNegated) return null
                        if (!statement.leftHandSide.isNameReferenceTo(lambdaParameterName)) return null
                        val rightTypeReference = statement.typeReference ?: return null

                        val bindingContext = callExpression.analyze(BodyResolveMode.PARTIAL)
                        val rightType = bindingContext[BindingContext.TYPE, rightTypeReference]
                        val resolvedCall = callExpression.getResolvedCall(bindingContext)

                        if (resolvedCall != null && rightType != null) {
                            val resultingElementType = resolvedCall.resultingDescriptor.returnType
                                ?.arguments?.singleOrNull()?.takeIf { !it.isStarProjection }?.type
                            if (resultingElementType != null && !rightType.isSubtypeOf(resultingElementType)) {
                                return null
                            }
                        }

                        return "filterIsInstance<${rightTypeReference.text}>()"
                    }
                }
                return null
            }, callChecker = fun(resolvedCall: ResolvedCall<*>): Boolean {
                val extensionReceiverType = resolvedCall.extensionReceiver?.type ?: return false
                return extensionReceiverType.constructor.declarationDescriptor?.defaultType?.isMap(extensionReceiverType.builtIns) == false
            })
        )
    }

    private class SimplifyCallFix(val conversion: Conversion, val replacement: String) : LocalQuickFix {
        override fun getName() = "Convert '${conversion.shortName}' call to '$replacement'"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val callExpression = descriptor.psiElement.parent as? KtCallExpression ?: return
            callExpression.replace(KtPsiFactory(callExpression).createExpression(replacement))
        }
    }
}

