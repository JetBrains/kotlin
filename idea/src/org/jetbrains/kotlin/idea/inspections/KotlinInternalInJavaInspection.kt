/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.lexer.KtTokens.INTERNAL_KEYWORD
import org.jetbrains.kotlin.psi.KtModifierListOwner


class KotlinInternalInJavaInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitReferenceExpression(expression: PsiReferenceExpression?) {
                expression?.checkAndReport(holder)
            }

            override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement?) {
                reference?.checkAndReport(holder)
            }
        }
    }

    private fun PsiElement.checkAndReport(holder: ProblemsHolder) {
        val lightElement = (this as? PsiReference)?.resolve() as? KtLightElement<*, *> ?: return
        val modifierListOwner = lightElement.kotlinOrigin as? KtModifierListOwner ?: return
        if (inSameModule(modifierListOwner)) {
            return
        }

        if (modifierListOwner.hasModifier(INTERNAL_KEYWORD)) {
            holder.registerProblem(this, "Usage of Kotlin internal declaration from different module")
        }
    }

    private fun PsiElement.inSameModule(element: PsiElement) = module?.equals(element.module) ?: true
}
