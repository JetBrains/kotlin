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
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.classInitializerVisitor
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class RedundantEmptyInitializerBlockInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = classInitializerVisitor(fun(initializer) {
        val body = initializer.body as? KtBlockExpression ?: return
        if (body.statements.isNotEmpty()) return
        holder.registerProblem(
            initializer,
            "Redundant empty initializer block",
            ProblemHighlightType.LIKE_UNUSED_SYMBOL,
            TextRange(initializer.initKeyword.startOffset, initializer.endOffset).shiftLeft(initializer.startOffset),
            RemoveInitializerBlockFix()
        )
    })

    private class RemoveInitializerBlockFix : LocalQuickFix {
        override fun getName() = "Remove initializer block"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val initializer = descriptor.psiElement as? KtClassInitializer ?: return
            val body = initializer.body as? KtBlockExpression ?: return
            
            val commentsBeforeInitKeyword = initializer.allChildren.takeWhile { it is PsiComment || it is PsiWhiteSpace }.let {
                val first = it.firstOrNull()
                if (first != null) PsiChildRange(first, it.lastOrNull()) else null
            }
            val commentsInBody = body.allChildren.let { child -> 
                val first = child.firstOrNull { it is PsiComment }
                if (first != null) PsiChildRange(first, child.lastOrNull { it is PsiComment }) else null
            }
            val parent = initializer.parent
            if (commentsInBody != null) {
                parent.addRangeAfter(commentsInBody.first, commentsInBody.last, initializer)
            }
            if (commentsBeforeInitKeyword != null) {
                parent.addRangeAfter(commentsBeforeInitKeyword.first, commentsBeforeInitKeyword.last, initializer)
            }
            
            initializer.delete()
        }
    }
}