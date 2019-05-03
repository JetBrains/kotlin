/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.primaryConstructorVisitor
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.isPrivate

class DataClassPrivateConstructorInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return primaryConstructorVisitor { constructor ->
            if (constructor.containingClass()?.isData() == true && constructor.isPrivate()) {
                val keyword = constructor.modifierList?.getModifier(KtTokens.PRIVATE_KEYWORD) ?: return@primaryConstructorVisitor
                val problemDescriptor = holder.manager.createProblemDescriptor(
                        keyword,
                        keyword,
                        "Private data class constructor is exposed via the generated 'copy' method.",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        isOnTheFly
                )

                holder.registerProblem(problemDescriptor)
            }
        }
    }
}