/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.scopes.utils.findFunction
import org.jetbrains.kotlin.resolve.scopes.utils.findVariable

class RedundantCompanionReferenceInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return referenceExpressionVisitor(fun(expression) {
            if (isRedundantCompanionReference(expression)) {
                holder.registerProblem(
                    expression,
                    "Redundant Companion reference",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    RemoveRedundantCompanionReferenceFix()
                )
            }
        })
    }

    companion object {
        fun isRedundantCompanionReference(reference: KtReferenceExpression): Boolean {
            val parent = reference.parent as? KtDotQualifiedExpression ?: return false
            val selectorExpression = parent.selectorExpression
            if (reference == selectorExpression && parent.parent !is KtDotQualifiedExpression) return false
            if (parent.getStrictParentOfType<KtImportDirective>() != null) return false

            val objectDeclaration = reference.mainReference.resolve() as? KtObjectDeclaration ?: return false
            if (!objectDeclaration.isCompanion()) return false
            if (reference.text != objectDeclaration.name) return false

            val context = reference.analyze()

            val containingClass = objectDeclaration.containingClass() ?: return false
            if (reference.containingClass() != containingClass && reference == parent.receiverExpression) return false
            val containingClassDescriptor = containingClass.descriptor as? ClassDescriptor ?: return false
            val selectorDescriptor = selectorExpression?.getResolvedCall(context)?.resultingDescriptor
            when (selectorDescriptor) {
                is PropertyDescriptor -> {
                    val name = selectorDescriptor.name
                    if (containingClassDescriptor.findMemberVariable(name) != null) return false
                    val variable = reference.getResolutionScope().findVariable(name, NoLookupLocation.FROM_IDE)
                    if (variable != null && variable.isLocalOrExtension(containingClassDescriptor)) return false
                }
                is FunctionDescriptor -> {
                    val name = selectorDescriptor.name
                    if (containingClassDescriptor.findMemberFunction(name) != null) return false
                    val function = reference.getResolutionScope().findFunction(name, NoLookupLocation.FROM_IDE)
                    if (function != null && function.isLocalOrExtension(containingClassDescriptor)) return false
                }
            }

            (reference as? KtSimpleNameExpression)?.getReceiverExpression()?.getQualifiedElementSelector()
                ?.mainReference?.resolveToDescriptors(context)?.firstOrNull()
                ?.let { if (it != containingClassDescriptor) return false }

            val grandParent = parent.parent as? KtQualifiedExpression
            if (grandParent != null) {
                val grandParentDescriptor = grandParent.getResolvedCall(context)?.resultingDescriptor ?: return false
                if (grandParentDescriptor is ConstructorDescriptor || grandParentDescriptor is FakeCallableDescriptorForObject) return false
            }
            return true
        }
    }
}

private fun <D : MemberDescriptor> ClassDescriptor.findMemberByName(name: Name, find: ClassDescriptor.(Name) -> D?): D? {
    val member = find(name)
    if (member != null) return member

    val memberInSuperClass = getSuperClassNotAny()?.findMemberByName(name, find)
    if (memberInSuperClass != null) return memberInSuperClass

    getSuperInterfaces().forEach {
        val memberInInterface = it.findMemberByName(name, find)
        if (memberInInterface != null) return memberInInterface
    }

    return null
}

private fun ClassDescriptor.findMemberVariable(name: Name): PropertyDescriptor? = findMemberByName(name) {
    unsubstitutedMemberScope.getContributedVariables(it, NoLookupLocation.FROM_IDE).firstOrNull()
}

private fun ClassDescriptor.findMemberFunction(name: Name): FunctionDescriptor? = findMemberByName(name) {
    unsubstitutedMemberScope.getContributedFunctions(it, NoLookupLocation.FROM_IDE).firstOrNull()
}

private fun CallableDescriptor.isLocalOrExtension(extensionClassDescriptor: ClassDescriptor): Boolean {
    return visibility == Visibilities.LOCAL ||
            extensionReceiverParameter?.type?.constructor?.declarationDescriptor == extensionClassDescriptor
}

class RemoveRedundantCompanionReferenceFix : LocalQuickFix {
    override fun getName() = "Remove redundant Companion reference"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val expression = descriptor.psiElement as? KtReferenceExpression ?: return
        removeRedundantCompanionReference(expression)
    }

    companion object {
        fun removeRedundantCompanionReference(expression: KtReferenceExpression) {
            val parent = expression.parent as? KtDotQualifiedExpression ?: return
            val selector = parent.selectorExpression ?: return
            val receiver = parent.receiverExpression
            if (expression == receiver) parent.replace(selector) else parent.replace(receiver)
        }
    }
}