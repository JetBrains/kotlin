/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.addRemoveModifier.sortModifiers
import org.jetbrains.kotlin.psi.psiUtil.allChildren

class SortModifiersInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitModifierList(list: KtModifierList) {
                super.visitModifierList(list)

                val modifierElements = list.allChildren.toList()
                var modifiersBeforeAnnotations = false
                var seenModifiers = false
                for (modifierElement in modifierElements) {
                    if (modifierElement.node.elementType is KtModifierKeywordToken) {
                        seenModifiers = true
                    } else if (seenModifiers && (modifierElement is KtAnnotationEntry || modifierElement is KtAnnotation)) {
                        modifiersBeforeAnnotations = true
                    }
                }

                val modifiers = modifierElements.mapNotNull { it.node.elementType as? KtModifierKeywordToken }.toList()
                if (modifiers.isEmpty()) return

                val sortedModifiers = sortModifiers(modifiers)
                if (modifiers == sortedModifiers && !modifiersBeforeAnnotations) return

                val message = if (modifiersBeforeAnnotations)
                    "Modifiers should follow annotations"
                else
                    "Non-canonical modifiers order"
                holder.registerProblem(
                    list,
                    message,
                    ProblemHighlightType.WEAK_WARNING,
                    SortModifiersFix(sortedModifiers)
                )
            }
        }
    }
}

private class SortModifiersFix(private val modifiers: List<KtModifierKeywordToken>) : LocalQuickFix {
    override fun getName() = "Sort modifiers"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val list = descriptor.psiElement as? KtModifierList ?: return
        val owner = list.parent as? KtModifierListOwner ?: return

        modifiers.forEach { owner.removeModifier(it) }
        modifiers.forEach { owner.addModifier(it) }
    }
}

