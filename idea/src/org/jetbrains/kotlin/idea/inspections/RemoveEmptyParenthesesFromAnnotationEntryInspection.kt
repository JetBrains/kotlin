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
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.valueArgumentListVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class RemoveEmptyParenthesesFromAnnotationEntryInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        valueArgumentListVisitor(fun(list) {
            if (list.arguments.isNotEmpty()) return
            val annotationEntry = list.parent as? KtAnnotationEntry ?: return
            if (annotationEntry.typeArguments.isNotEmpty()) return

            val annotationClassDescriptor = annotationEntry.getAnnotationClassDescriptor() ?: return

            // if all annotation constructors must receive at least one argument
            // then parentheses *are* necessary and inspection should not trigger
            if (annotationClassDescriptor.constructors.all { it.hasParametersWithoutDefault() }) return

            holder.registerProblem(
                list,
                "Parentheses should be removed",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                RemoveEmptyParenthesesFromAnnotationEntryFix()
            )
        })

    private fun KtAnnotationEntry.getAnnotationClassDescriptor(): ClassDescriptor? {
        val context = analyze(BodyResolveMode.PARTIAL)
        return context[BindingContext.ANNOTATION, this]?.annotationClass
    }

    private fun ClassConstructorDescriptor.hasParametersWithoutDefault() = valueParameters.any { !it.declaresDefaultValue() }

}

private class RemoveEmptyParenthesesFromAnnotationEntryFix : LocalQuickFix {

    override fun getName() = "Remove unnecessary parentheses"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        (descriptor.psiElement as? KtValueArgumentList)?.delete()
    }

}
