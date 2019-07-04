/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.core.implicitVisibility
import org.jetbrains.kotlin.idea.quickfix.RemoveModifierFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.declarationVisitor
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier

class RedundantVisibilityModifierInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return declarationVisitor { declaration ->
            if (declaration is KtPropertyAccessor && declaration.isGetter) return@declarationVisitor // There is a quick fix for REDUNDANT_MODIFIER_IN_GETTER
            val visibilityModifier = declaration.visibilityModifier() ?: return@declarationVisitor
            val implicitVisibility = declaration.implicitVisibility()
            val redundantVisibility = when {
                visibilityModifier.node.elementType == implicitVisibility ->
                    implicitVisibility
                declaration.hasModifier(KtTokens.INTERNAL_KEYWORD) && declaration.containingClassOrObject?.isLocal == true ->
                    KtTokens.INTERNAL_KEYWORD
                else ->
                    null
            }
            if (redundantVisibility != null) {
                holder.registerProblem(
                    visibilityModifier,
                    "Redundant visibility modifier",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    IntentionWrapper(
                        RemoveModifierFix(declaration, redundantVisibility, isRedundant = true),
                        declaration.containingFile
                    )
                )
            }
        }
    }
}
