/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.CleanupLocalInspectionTool
import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.intentions.RemoveExplicitTypeIntention
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.namedFunctionVisitor
import org.jetbrains.kotlin.types.typeUtil.isUnit

class RedundantUnitReturnTypeInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return namedFunctionVisitor(fun(function) {
            if (function.containingFile is KtCodeFragment) return
            val typeElement = function.typeReference?.typeElement ?: return
            if (hasRedundantUnitReturnType(function)) {
                holder.registerProblem(
                    typeElement,
                    "Redundant 'Unit' return type",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    IntentionWrapper(RemoveExplicitTypeIntention(), function.containingKtFile)
                )
            }
        })
    }

    companion object {
        fun hasRedundantUnitReturnType(function: KtNamedFunction): Boolean {
            if (!function.hasBlockBody()) return false
            if (function.typeReference?.typeElement == null) return false
            val descriptor = function.resolveToDescriptorIfAny() ?: return false
            return descriptor.returnType?.isUnit() == true
        }
    }
}