/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.quickfix.RemoveUnusedFunctionParameterFix
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.parameterVisitor
import org.jetbrains.kotlin.resolve.BindingContext.UNUSED_MAIN_PARAMETER

class UnusedMainParameterInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
        parameterVisitor(fun(parameter: KtParameter) {
            val function = parameter.ownerFunction as? KtNamedFunction ?: return
            if (function.name != "main") return
            val context = function.analyzeWithContent()
            if (context[UNUSED_MAIN_PARAMETER, parameter] == true) {
                holder.registerProblem(
                    parameter,
                    "Since Kotlin 1.3 main parameter is not necessary",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    IntentionWrapper(RemoveUnusedFunctionParameterFix(parameter), parameter.containingFile)
                )
            }
        })
}