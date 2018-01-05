/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.expressionVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isDynamic

class UnsafeCastFromDynamicInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return expressionVisitor(fun(expression) {
            val context = expression.analyze(BodyResolveMode.PARTIAL)
            val expectedType = context[BindingContext.EXPECTED_EXPRESSION_TYPE, expression] ?: return
            val actualType = expression.getType(context) ?: return

            if (actualType.isDynamic() && !expectedType.isDynamic() && !TypeUtils.noExpectedType(expectedType)) {
                holder.registerProblem(expression, "Implicit (unsafe) cast from dynamic to $expectedType")
            }
        })
    }
}
