/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl.WithDestructuringDeclaration
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.getCallableDescriptor
import org.jetbrains.kotlin.idea.refactoring.getThisLabelName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull

class ForEachParameterNotUsedInspection : AbstractKotlinInspection() {
    companion object {
        private const val EXPRESSION_NAME = "forEach"
        private val COLLECTIONS_FOREACH_FQNAME = FqName("kotlin.collections.$EXPRESSION_NAME")
        private val SEQUENCES_FOREACH_FQNAME = FqName("kotlin.sequences.$EXPRESSION_NAME")
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return callExpressionVisitor {
            val calleeExpression = it.calleeExpression
            if (calleeExpression?.text != EXPRESSION_NAME) return@callExpressionVisitor

            when (it.getCallableDescriptor()?.fqNameOrNull()) {
                COLLECTIONS_FOREACH_FQNAME, SEQUENCES_FOREACH_FQNAME -> {
                    val lambda = it.lambdaArguments.singleOrNull()?.getLambdaExpression() ?: return@callExpressionVisitor
                    val descriptor = lambda.analyze()[BindingContext.FUNCTION, lambda.functionLiteral] ?: return@callExpressionVisitor
                    val iterableParameter = descriptor.valueParameters.singleOrNull() ?: return@callExpressionVisitor

                    if (iterableParameter !is WithDestructuringDeclaration &&
                        lambda.bodyExpression?.usesDescriptor(iterableParameter) != true) {

                        holder.registerProblem(
                            calleeExpression,
                            "Loop parameter '${iterableParameter.getThisLabelName()}' is unused",
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                        )
                    }
                }
            }
        }
    }

    private fun KtBlockExpression.usesDescriptor(descriptor: VariableDescriptor): Boolean {
        var used = false
        acceptChildren(object : KtVisitorVoid() {
            override fun visitKtElement(element: KtElement) {
                if (!used) {
                    if (element is KtNameReferenceExpression) {
                        used = element.isUsed()
                    } else if (element.children.isNotEmpty()) {
                        element.acceptChildren(this)
                    }
                }
            }

            private fun KtNameReferenceExpression.isUsed(): Boolean {
                val bindingContext = analyze()
                val resolvedCall = getResolvedCall(bindingContext) ?: return false

                return (resolvedCall is VariableAsFunctionResolvedCall && resolvedCall.variableCall.candidateDescriptor == descriptor) ||
                        (resolvedCall.candidateDescriptor == descriptor)
            }
        })
        return used
    }
}