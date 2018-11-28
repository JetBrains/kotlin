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
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl.WithDestructuringDeclaration
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.SpecifyExplicitLambdaSignatureIntention
import org.jetbrains.kotlin.idea.refactoring.getThisLabelName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ForEachParameterNotUsedInspection : AbstractKotlinInspection() {
    companion object {
        private const val FOREACH_NAME = "forEach"
        private val COLLECTIONS_FOREACH_FQNAME = FqName("kotlin.collections.$FOREACH_NAME")
        private val SEQUENCES_FOREACH_FQNAME = FqName("kotlin.sequences.$FOREACH_NAME")
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return callExpressionVisitor {
            val calleeExpression = it.calleeExpression as? KtNameReferenceExpression
            if (calleeExpression?.getReferencedName() != FOREACH_NAME) return@callExpressionVisitor
            val lambda = it.lambdaArguments.singleOrNull()?.getLambdaExpression()
            if (lambda == null || lambda.functionLiteral.arrow != null) return@callExpressionVisitor
            val context = it.analyze()
            when (it.getResolvedCall(context)?.resultingDescriptor?.fqNameOrNull()) {
                COLLECTIONS_FOREACH_FQNAME, SEQUENCES_FOREACH_FQNAME -> {
                    val descriptor = context[BindingContext.FUNCTION, lambda.functionLiteral] ?: return@callExpressionVisitor
                    val iterableParameter = descriptor.valueParameters.singleOrNull() ?: return@callExpressionVisitor

                    if (iterableParameter !is WithDestructuringDeclaration &&
                        !lambda.bodyExpression.usesDescriptor(iterableParameter, context)
                    ) {
                        holder.registerProblem(
                            calleeExpression,
                            "Loop parameter '${iterableParameter.getThisLabelName()}' is unused",
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                            IntroduceAnonymousParameterFix()
                        )
                    }
                }
            }
        }
    }

    private class IntroduceAnonymousParameterFix : LocalQuickFix {
        override fun getFamilyName() = "Introduce anonymous parameter"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val callExpression = descriptor.psiElement.parent as? KtCallExpression ?: return
            val literal = callExpression.lambdaArguments.singleOrNull()?.getLambdaExpression()?.functionLiteral ?: return
            val psiFactory = KtPsiFactory(project)
            val newParameterList = psiFactory.createLambdaParameterList("_")
            with(SpecifyExplicitLambdaSignatureIntention) {
                literal.setParameterListIfAny(psiFactory, newParameterList)
            }
        }
    }

    private fun KtBlockExpression?.usesDescriptor(descriptor: VariableDescriptor, context: BindingContext): Boolean {
        if (this == null) return false
        var used = false
        acceptChildren(object : KtVisitorVoid() {
            override fun visitKtElement(element: KtElement) {
                if (!used) {
                    if (element.children.isNotEmpty()) {
                        element.acceptChildren(this)
                    } else {
                        val resolvedCall = element.getResolvedCall(context) ?: return

                        used = descriptor == when (resolvedCall) {
                            is VariableAsFunctionResolvedCall -> resolvedCall.variableCall.candidateDescriptor
                            else -> resolvedCall.candidateDescriptor
                        }
                    }
                }
            }
        })
        return used
    }
}