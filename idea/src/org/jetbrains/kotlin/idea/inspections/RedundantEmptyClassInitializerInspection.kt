/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.classInitializerVisitor
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespace
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class RedundantEmptyClassInitializerInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = classInitializerVisitor(fun(initializer) {
        if (initializer.getStrictParentOfType<KtClassOrObject>() == null) return
        val body = initializer.body as? KtBlockExpression ?: return
        val leftBrace = body.lBrace ?: return
        val rightBrace = body.rBrace ?: return
        if (leftBrace.getNextSiblingIgnoringWhitespace() != rightBrace) return 
        holder.registerProblem(
            initializer.initKeyword,
            "Redundant empty class initializer",
            ProblemHighlightType.LIKE_UNUSED_SYMBOL,
            RemoveInitializerFix()
        )
    })

    private class RemoveInitializerFix : LocalQuickFix {
        override fun getName() = "Remove initializer"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            descriptor.psiElement.parent.delete()
        }
    }
}