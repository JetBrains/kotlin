/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.CleanupLocalInspectionTool
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotation
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.addRemoveModifier.sortModifiers
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class SortModifiersInspection : AbstractApplicabilityBasedInspection<KtModifierList>(
    KtModifierList::class.java
), CleanupLocalInspectionTool {
    override fun isApplicable(element: KtModifierList): Boolean {
        val modifiers = element.modifierKeywordTokens()
        if (modifiers.isEmpty()) return false
        val sortedModifiers = sortModifiers(modifiers)
        if (modifiers == sortedModifiers && !element.modifiersBeforeAnnotations()) return false
        return true
    }

    override fun inspectionHighlightRangeInElement(element: KtModifierList): TextRange? {
        val modifierElements = element.allChildren.toList()
        val startElement = modifierElements.firstOrNull { it.node.elementType is KtModifierKeywordToken } ?: return null
        val endElement = modifierElements.lastOrNull { it.node.elementType is KtModifierKeywordToken } ?: return null
        return TextRange(startElement.startOffset, endElement.endOffset).shiftLeft(element.startOffset)
    }

    override fun inspectionText(element: KtModifierList) =
        if (element.modifiersBeforeAnnotations()) "Modifiers should follow annotations" else "Non-canonical modifiers order"

    override val defaultFixText = "Sort modifiers"

    override fun applyTo(element: KtModifierList, project: Project, editor: Editor?) {
        val owner = element.parent as? KtModifierListOwner ?: return
        val sortedModifiers = sortModifiers(element.modifierKeywordTokens())
        val existingModifiers = sortedModifiers.filter { owner.hasModifier(it) }
        existingModifiers.forEach { owner.removeModifier(it) }
        // We add visibility / modality modifiers after all others,
        // because they can be redundant or not depending on others (e.g. override)
        existingModifiers
            .partition { it in KtTokens.VISIBILITY_MODIFIERS || it in KtTokens.MODALITY_MODIFIERS }
            .let { it.second + it.first }
            .forEach { owner.addModifier(it) }
    }

    private fun KtModifierList.modifierKeywordTokens(): List<KtModifierKeywordToken> {
        return allChildren.mapNotNull { it.node.elementType as? KtModifierKeywordToken }.toList()
    }

    private fun KtModifierList.modifiersBeforeAnnotations(): Boolean {
        val modifierElements = this.allChildren.toList()
        var modifiersBeforeAnnotations = false
        var seenModifiers = false
        for (modifierElement in modifierElements) {
            if (modifierElement.node.elementType is KtModifierKeywordToken) {
                seenModifiers = true
            } else if (seenModifiers && (modifierElement is KtAnnotationEntry || modifierElement is KtAnnotation)) {
                modifiersBeforeAnnotations = true
            }
        }
        return modifiersBeforeAnnotations
    }
}
