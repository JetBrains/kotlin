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
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.idea.core.setType
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.namedFunctionVisitor
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class MainFunctionReturnUnitInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return namedFunctionVisitor(fun(function: KtNamedFunction) {
            if (function.name != "main") return

            val descriptor = function.descriptor as? FunctionDescriptor ?: return
            if (!MainFunctionDetector.isMain(descriptor, checkReturnType = false)) return
            if (MainFunctionDetector.isMainReturnType(descriptor)) return

            holder.registerProblem(
                function.nameIdentifier ?: function,
                "main should return Unit",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                ChangeMainFunctionReturnTypeToUnitFix(function.typeReference != null)
            )
        })
    }
}

private class ChangeMainFunctionReturnTypeToUnitFix(private val hasExplicitReturnType: Boolean) : LocalQuickFix {
    override fun getName() = if (hasExplicitReturnType) "Change return type to Unit" else "Add explicit Unit return type"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val function = descriptor.psiElement.getNonStrictParentOfType<KtNamedFunction>() ?: return
        if (function.hasBlockBody()) {
            function.typeReference = null
        } else {
            function.setType(KotlinBuiltIns.FQ_NAMES.unit.asString())
        }
    }
}