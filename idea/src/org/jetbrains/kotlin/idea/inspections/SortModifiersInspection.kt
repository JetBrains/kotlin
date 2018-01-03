/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

