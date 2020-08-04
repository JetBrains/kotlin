/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.analysis.analyzeAsReplacement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.isReferenceToBuiltInEnumFunction
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.resolveToDescriptors
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.scopes.utils.findFunction
import org.jetbrains.kotlin.resolve.scopes.utils.findVariable
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

class RedundantCompanionReferenceInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return referenceExpressionVisitor(fun(expression) {
            if (isRedundantCompanionReference(expression)) {
                holder.registerProblem(
                    expression,
                    KotlinBundle.message("redundant.companion.reference"),
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    RemoveRedundantCompanionReferenceFix()
                )
            }
        })
    }

    private fun isRedundantCompanionReference(reference: KtReferenceExpression): Boolean {
        val parent = reference.parent as? KtDotQualifiedExpression ?: return false
        val grandParent = parent.parent as? KtElement
        val selectorExpression = parent.selectorExpression
        if (reference == selectorExpression && grandParent !is KtDotQualifiedExpression) return false
        if (parent.getStrictParentOfType<KtImportDirective>() != null) return false

        val objectDeclaration = reference.mainReference.resolve() as? KtObjectDeclaration ?: return false
        if (!objectDeclaration.isCompanion()) return false
        val referenceText = reference.text
        if (referenceText != objectDeclaration.name) return false
        if (reference != selectorExpression && referenceText == (selectorExpression as? KtNameReferenceExpression)?.text) return false

        val containingClass = objectDeclaration.containingClass() ?: return false
        if (reference.containingClass() != containingClass && reference == parent.receiverExpression) return false
        if (parent.isReferenceToBuildInEnumFunctionInEnumClass(containingClass)) return false

        val context = reference.analyze()
        if (grandParent.isReferenceToClassOrObject(context)) return false

        val containingClassDescriptor =
            context[BindingContext.DECLARATION_TO_DESCRIPTOR, containingClass] as? ClassDescriptor ?: return false
        if (containingClassDescriptor.hasSameNameMemberAs(selectorExpression, context)) return false

        (reference as? KtSimpleNameExpression)?.getReceiverExpression()?.getQualifiedElementSelector()
            ?.mainReference?.resolveToDescriptors(context)?.firstOrNull()
            ?.let { if (it != containingClassDescriptor) return false }

        if (selectorExpression is KtCallExpression && referenceText == selectorExpression.calleeExpression?.text) {
            val newExpression = KtPsiFactory(reference).createExpressionByPattern("$0", selectorExpression)
            val newContext = newExpression.analyzeAsReplacement(parent, context)
            val descriptor = newExpression.getResolvedCall(newContext)?.resultingDescriptor as? FunctionDescriptor
            if (descriptor?.isOperator == true) return false
        }

        return true
    }

    private fun KtDotQualifiedExpression.isReferenceToBuildInEnumFunctionInEnumClass(containingClass: KtClass): Boolean {
        return containingClass.isEnum()
                && (isReferenceToBuiltInEnumFunction() || (parent as? KtElement)?.isReferenceToBuiltInEnumFunction() == true)
    }

    private fun KtElement?.isReferenceToClassOrObject(context: BindingContext): Boolean {
        if (this !is KtQualifiedExpression) return false
        val descriptor = getResolvedCall(context)?.resultingDescriptor
        return descriptor == null || descriptor is ConstructorDescriptor || descriptor is FakeCallableDescriptorForObject
    }

    private fun ClassDescriptor.hasSameNameMemberAs(expression: KtExpression?, context: BindingContext): Boolean {
        when (val descriptor = expression?.getResolvedCall(context)?.resultingDescriptor) {
            is PropertyDescriptor -> {
                val name = descriptor.name
                if (findMemberVariable(name) != null) return true

                val type = descriptor.type
                val javaGetter = findMemberFunction(Name.identifier(JvmAbi.getterName(name.asString())))
                    ?.takeIf { f -> f is JavaMethodDescriptor || f.overriddenDescriptors.any { it is JavaMethodDescriptor } }
                if (javaGetter?.valueParameters?.isEmpty() == true && javaGetter.returnType?.makeNotNullable() == type) return true

                val variable = expression.getResolutionScope().findVariable(name, NoLookupLocation.FROM_IDE)
                if (variable != null && variable.isLocalOrExtension(this)) return true
            }
            is FunctionDescriptor -> {
                val name = descriptor.name
                if (findMemberFunction(name) != null) return true
                val function = expression.getResolutionScope().findFunction(name, NoLookupLocation.FROM_IDE)
                if (function != null && function.isLocalOrExtension(this)) return true
            }
        }
        return false
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
        return visibility == DescriptorVisibilities.LOCAL ||
                extensionReceiverParameter?.type?.constructor?.declarationDescriptor == extensionClassDescriptor
    }

    private class RemoveRedundantCompanionReferenceFix : LocalQuickFix {
        override fun getName() = KotlinBundle.message("remove.redundant.companion.reference.fix.text")

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
}
