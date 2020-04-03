/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.MovePropertyToConstructorIntention
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.hasUsages
import org.jetbrains.kotlin.idea.refactoring.isInterfaceClass
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class CanBePrimaryConstructorPropertyInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return propertyVisitor(fun(property) {
            if (property.isLocal) return
            if (property.getter != null || property.setter != null || property.delegate != null) return
            val assigned = property.initializer as? KtReferenceExpression ?: return

            val context = assigned.analyze()
            val assignedDescriptor = context.get(BindingContext.REFERENCE_TARGET, assigned) as? ValueParameterDescriptor ?: return

            val containingConstructor = assignedDescriptor.containingDeclaration as? ClassConstructorDescriptor ?: return
            if (containingConstructor.containingDeclaration.isData) return

            val propertyTypeReference = property.typeReference
            val propertyType = context.get(BindingContext.TYPE, propertyTypeReference)
            if (propertyType != null && propertyType != assignedDescriptor.type) return

            val nameIdentifier = property.nameIdentifier ?: return
            if (nameIdentifier.text != assignedDescriptor.name.asString()) return

            val assignedParameter = DescriptorToSourceUtils.descriptorToDeclaration(assignedDescriptor) as? KtParameter ?: return
            val containingClassOrObject = property.containingClassOrObject ?: return
            if (containingClassOrObject !== assignedParameter.containingClassOrObject) return
            if (containingClassOrObject.isInterfaceClass()) return
            if (property.hasModifier(KtTokens.OPEN_KEYWORD)
                && containingClassOrObject is KtClass
                && containingClassOrObject.isOpen()
                && assignedParameter.isUsedInClassInitializer(containingClassOrObject)
            ) return

            holder.registerProblem(
                holder.manager.createProblemDescriptor(
                    nameIdentifier,
                    nameIdentifier,
                    KotlinBundle.message("property.is.explicitly.assigned.to.parameter.0.can", assignedDescriptor.name),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    isOnTheFly,
                    MovePropertyToConstructorIntention()
                )
            )
        })
    }

    private fun KtClass.isOpen(): Boolean {
        return hasModifier(KtTokens.OPEN_KEYWORD) || hasModifier(KtTokens.ABSTRACT_KEYWORD) || hasModifier(KtTokens.SEALED_KEYWORD)
    }

    private fun KtParameter.isUsedInClassInitializer(containingClass: KtClass): Boolean {
        val classInitializer = containingClass.body?.declarations?.firstIsInstanceOrNull<KtClassInitializer>() ?: return false
        return hasUsages(classInitializer)
    }
}