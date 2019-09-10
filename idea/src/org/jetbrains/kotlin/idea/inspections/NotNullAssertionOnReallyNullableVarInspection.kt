/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.calleeName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.dotQualifiedExpressionVisitor
import org.jetbrains.kotlin.types.TypeUtils

class NotNullAssertionOnReallyNullableVarInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return dotQualifiedExpressionVisitor(fun(expression) {
            val postfixExpression = expression.firstChild as? KtPostfixExpression ?: return
            if(postfixExpression.operationToken != KtTokens.EXCLEXCL) return

            holder.registerProblem(
                postfixExpression,
                "Unsafe using of '!!' operator",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        })
    }
}