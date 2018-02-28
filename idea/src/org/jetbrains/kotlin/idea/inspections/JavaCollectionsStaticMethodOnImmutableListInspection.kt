/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.dotQualifiedExpressionVisitor

class JavaCollectionsStaticMethodOnImmutableListInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return dotQualifiedExpressionVisitor(fun(expression) {
            val (methodName, firstArg) = JavaCollectionsStaticMethodInspection.getTargetMethodOnImmutableList(expression) ?: return
            holder.registerProblem(
                expression,
                "The '${firstArg.text}' may be changed with Java Collections method '$methodName'"
            )
        })
    }
}
