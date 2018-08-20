/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.intentions.getCallableDescriptor
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.findFunctionByName
import org.jetbrains.kotlin.psi.psiUtil.findPropertyByName
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.scopes.utils.findFunction
import org.jetbrains.kotlin.resolve.scopes.utils.findVariable

class RedundantCompanionReferenceInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return referenceExpressionVisitor(fun(expression) {
            val parent = expression.parent as? KtDotQualifiedExpression ?: return
            val selectorExpression = parent.selectorExpression
            if (expression == selectorExpression && parent.parent !is KtDotQualifiedExpression) return
            if (parent.getStrictParentOfType<KtImportDirective>() != null) return

            val objectDeclaration = expression.mainReference.resolve() as? KtObjectDeclaration ?: return
            if (!objectDeclaration.isCompanion()) return
            if (expression.text != objectDeclaration.name) return

            val containingClass = objectDeclaration.containingClass() ?: return
            val containingClassDescriptor = containingClass.descriptor as? ClassDescriptor ?: return
            val selectorDescriptor = selectorExpression?.getCallableDescriptor()
            when (selectorDescriptor) {
                is PropertyDescriptor -> {
                    val name = selectorDescriptor.name
                    if (containingClass.findPropertyByName(name.asString()) != null) return
                    val variable = expression.getResolutionScope().findVariable(name, NoLookupLocation.FROM_IDE)
                    if (variable != null && variable.isLocalOrExtension(containingClassDescriptor)) return
                }
                is FunctionDescriptor -> {
                    val name = selectorDescriptor.name
                    val function = containingClass.findFunctionByName(name.asString())?.descriptor
                            ?: expression.getResolutionScope().findFunction(name, NoLookupLocation.FROM_IDE)?.takeIf {
                                it.isLocalOrExtension(containingClassDescriptor)
                            }
                    if (function is FunctionDescriptor) {
                        val functionParams = function.valueParameters
                        val calleeParams =
                            (selectorExpression as? KtCallExpression)?.calleeExpression?.getCallableDescriptor()?.valueParameters.orEmpty()
                        if (functionParams.size == calleeParams.size &&
                            functionParams.zip(calleeParams).all { it.first.type == it.second.type }
                        ) return
                    }
                }
            }

            val grandParent = parent.parent as? KtQualifiedExpression
            if (grandParent != null) {
                val grandParentDescriptor = grandParent.resolveToCall()?.resultingDescriptor ?: return
                if (grandParentDescriptor is ConstructorDescriptor || grandParentDescriptor is FakeCallableDescriptorForObject) return
            }

            holder.registerProblem(
                expression,
                "Redundant Companion reference",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                RemoveRedundantCompanionReferenceFix()
            )
        })
    }
}

private fun CallableDescriptor.isLocalOrExtension(extensionClassDescriptor: ClassDescriptor): Boolean {
    return visibility == Visibilities.LOCAL ||
            extensionReceiverParameter?.type?.constructor?.declarationDescriptor == extensionClassDescriptor
}

private class RemoveRedundantCompanionReferenceFix : LocalQuickFix {
    override fun getName() = "Remove redundant Companion reference"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val expression = descriptor.psiElement as? KtReferenceExpression ?: return
        val parent = expression.parent as? KtDotQualifiedExpression ?: return
        val selector = parent.selectorExpression ?: return
        val receiver = parent.receiverExpression
        if (expression == receiver) parent.replace(selector) else parent.replace(receiver)
    }
}