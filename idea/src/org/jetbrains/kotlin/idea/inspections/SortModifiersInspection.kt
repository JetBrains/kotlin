/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
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

                val modifiers = modifierElements.asSequence().mapNotNull { it.node.elementType as? KtModifierKeywordToken }.toList()
                if (modifiers.isEmpty()) return

                val startElement = modifierElements.firstOrNull { it.node.elementType is KtModifierKeywordToken } ?: return

                val sortedModifiers = sortModifiers(modifiers)
                if (modifiers == sortedModifiers && !modifiersBeforeAnnotations) return

                val message = if (modifiersBeforeAnnotations)
                    "Modifiers should follow annotations"
                else
                    "Non-canonical modifiers order"

                val descriptor = holder.manager.createProblemDescriptor(
                    startElement,
                    list,
                    message,
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    isOnTheFly,
                    SortModifiersFix(sortedModifiers)
                )
                holder.registerProblem(descriptor)
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
        // We add visibility / modality modifiers after all others,
        // because they can be redundant or not depending on others (e.g. override)
        modifiers
            .partition { it in KtTokens.VISIBILITY_MODIFIERS || it in KtTokens.MODALITY_MODIFIERS }
            .let { it.second + it.first }
            .forEach { owner.addModifier(it) }
    }
}

