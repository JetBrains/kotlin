/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.setType
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.primaryConstructorVisitor
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable

class SelfReferenceConstructorParameterInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = primaryConstructorVisitor(fun(constructor) {
        val parameter = constructor.valueParameterList?.selfReferenceParameter() ?: return
        val rangeInElement = parameter.typeReference?.textRange?.shiftRight(-parameter.startOffset) ?: return
        holder.registerProblem(
            parameter,
            rangeInElement,
            "Constructor has non-null self reference parameter",
            ConvertToNullableTypeFix()
        )
    })

    private fun KtParameterList.selfReferenceParameter(): KtParameter? {
        val containingClass = this.containingClass() ?: return null
        val className = containingClass.name ?: return null
        val parameter = this.parameters.firstOrNull { it.typeReference?.text == className } ?: return null

        val typeReference = parameter.typeReference ?: return null
        val context = analyze(BodyResolveMode.PARTIAL)
        val type = context[BindingContext.TYPE, typeReference] ?: return null
        if (type.isNullable()) return null
        if (type.constructor.declarationDescriptor != context[BindingContext.DECLARATION_TO_DESCRIPTOR, containingClass]) return null

        return parameter
    }

    private class ConvertToNullableTypeFix : LocalQuickFix {
        override fun getName() = "Convert to nullable type"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val parameter = descriptor.psiElement as? KtParameter ?: return
            val typeReference = parameter.typeReference ?: return
            val type = parameter.analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, typeReference] ?: return
            parameter.setType(type.makeNullable())
        }
    }
}