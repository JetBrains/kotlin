/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.core.implicitVisibility
import org.jetbrains.kotlin.idea.quickfix.RemoveModifierFix
import org.jetbrains.kotlin.psi.declarationVisitor
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier

class RedundantVisibilityModifierInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return declarationVisitor { declaration ->
            val visibilityModifier = declaration.visibilityModifier() ?: return@declarationVisitor
            val implicitVisibility = declaration.implicitVisibility()
            if (visibilityModifier.node.elementType == implicitVisibility) {
                holder.registerProblem(visibilityModifier,
                                       "Redundant visibility modifier",
                                       ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                                       IntentionWrapper(RemoveModifierFix(declaration, implicitVisibility, isRedundant = true),
                                                        declaration.containingFile))
            }
        }
    }
}
