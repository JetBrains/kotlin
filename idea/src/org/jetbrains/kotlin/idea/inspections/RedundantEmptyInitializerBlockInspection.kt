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
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.classInitializerVisitor

class RedundantEmptyInitializerBlockInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = classInitializerVisitor(fun(initializer) {
        val body = initializer.body as? KtBlockExpression ?: return
        if (body.statements.isNotEmpty()) return
        holder.registerProblem(
            initializer,
            "Redundant empty initializer block",
            ProblemHighlightType.LIKE_UNUSED_SYMBOL,
            RemoveInitializerBlockFix()
        )
    })

    private class RemoveInitializerBlockFix : LocalQuickFix {
        override fun getName() = "Remove initializer block"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            (descriptor.psiElement as? KtClassInitializer)?.delete()
        }
    }
}