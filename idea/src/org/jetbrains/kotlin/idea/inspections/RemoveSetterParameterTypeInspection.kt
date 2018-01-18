/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.intentions.RemoveExplicitTypeIntention
import org.jetbrains.kotlin.idea.intentions.isSetterParameter
import org.jetbrains.kotlin.psi.parameterVisitor

class RemoveSetterParameterTypeInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return parameterVisitor { dcl ->
            val typeReference = dcl.takeIf { it.isSetterParameter }?.typeReference ?: return@parameterVisitor
            holder.registerProblem(
                typeReference,
                "Redundant setter parameter type",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                IntentionWrapper(RemoveExplicitTypeIntention(), dcl.containingKtFile)
            )
        }
    }
}