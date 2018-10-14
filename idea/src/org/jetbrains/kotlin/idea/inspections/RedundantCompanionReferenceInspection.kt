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
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
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
            if (expression.containingClass() != containingClass && expression == parent.receiverExpression) return
            val containingClassDescriptor = containingClass.descriptor as? ClassDescriptor ?: return
            val selectorDescriptor = selectorExpression?.getCallableDescriptor()
            when (selectorDescriptor) {
                is PropertyDescriptor -> {
                    val name = selectorDescriptor.name
                    if (containingClassDescriptor.findMemberVariable(name.asString()) != null) return
                    val variable = expression.getResolutionScope().findVariable(name, NoLookupLocation.FROM_IDE)
                    if (variable != null && variable.isLocalOrExtension(containingClassDescriptor)) return
                }
                is FunctionDescriptor -> {
                    val name = selectorDescriptor.name
                    val functions = containingClassDescriptor.collectMemberFunction(name.asString()) + listOfNotNull(
                        expression.getResolutionScope().findFunction(name, NoLookupLocation.FROM_IDE)?.takeIf {
                            it.isLocalOrExtension(containingClassDescriptor)
                        }
                    )
                    if (functions.any {
                            val functionParams = it.valueParameters
                            val calleeParams = (selectorExpression as? KtCallExpression)?.calleeExpression?.getCallableDescriptor()
                                ?.valueParameters.orEmpty()
                            functionParams.size == calleeParams.size && functionParams.zip(calleeParams).all { param ->
                                param.first.type == param.second.type
                            }
                        }) return
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

private fun ClassDescriptor.findMemberVariable(name: String): PropertyDescriptor? {
    val variable = unsubstitutedMemberScope.getContributedVariables(Name.identifier(name), NoLookupLocation.FROM_IDE).firstOrNull()
    if (variable != null) return variable

    val variableInSuperClass = getSuperClassNotAny()?.findMemberVariable(name)
    if (variableInSuperClass != null) return variableInSuperClass

    getSuperInterfaces().forEach {
        val variableInInterface = it.findMemberVariable(name)
        if (variableInInterface != null) return variableInInterface
    }

    return null
}

private fun ClassDescriptor.collectMemberFunction(name: String): MutableList<FunctionDescriptor> {
    val functions = mutableListOf<FunctionDescriptor>()
    fun collect(descriptor: ClassDescriptor) {
        functions.addAll(descriptor.unsubstitutedMemberScope.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_IDE))
        descriptor.getSuperClassNotAny()?.let { collect(it) }
        descriptor.getSuperInterfaces().forEach { collect(it) }
    }
    collect(this)
    return functions
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