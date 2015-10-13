/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.psi.addRemoveModifier

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken
import org.jetbrains.kotlin.lexer.JetTokens.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.siblings

private fun createModifierList(text: String, owner: JetModifierListOwner): JetModifierList {
    val newModifierList = JetPsiFactory(owner).createModifierList(text)
    val anchor = owner.firstChild!!
            .siblings(forward = true)
            .dropWhile { it is PsiComment || it is PsiWhiteSpace }
            .first()
    return owner.addBefore(newModifierList, anchor) as JetModifierList
}

fun addModifier(owner: JetModifierListOwner, modifier: JetModifierKeywordToken) {
    val modifierList = owner.modifierList
    if (modifierList == null) {
        createModifierList(modifier.value, owner)
    }
    else {
        addModifier(modifierList, modifier)
    }
}

fun addAnnotationEntry(owner: JetModifierListOwner, annotationEntry: JetAnnotationEntry): JetAnnotationEntry {
    val modifierList = owner.modifierList
    return if (modifierList == null) {
        createModifierList(annotationEntry.text, owner).annotationEntries.first()
    }
    else {
        modifierList.addBefore(annotationEntry, modifierList.firstChild) as JetAnnotationEntry
    }
}

internal fun addModifier(modifierList: JetModifierList, modifier: JetModifierKeywordToken) {
    if (modifierList.hasModifier(modifier)) return

    val newModifier = JetPsiFactory(modifierList).createModifier(modifier)
    val modifierToReplace = MODIFIERS_TO_REPLACE[modifier]
            ?.map { modifierList.getModifier(it) }
            ?.filterNotNull()
            ?.firstOrNull()

    if (modifierToReplace != null) {
        modifierToReplace.replace(newModifier)
    }
    else {
        val newModifierOrder = MODIFIERS_ORDER.indexOf(modifier)

        fun placeAfter(child: PsiElement): Boolean {
            if (child is PsiWhiteSpace) return false
            if (child is JetAnnotation) return true // place modifiers after annotations
            val elementType = child.getNode()!!.getElementType()
            val order = MODIFIERS_ORDER.indexOf(elementType)
            return newModifierOrder > order
        }

        val lastChild = modifierList.getLastChild()
        val anchor = lastChild?.siblings(forward = false)?.firstOrNull(::placeAfter)
        modifierList.addAfter(newModifier, anchor)

        if (anchor == lastChild) { // add line break if needed, otherwise visibility keyword may appear on previous line
            val whiteSpace = modifierList.getNextSibling() as? PsiWhiteSpace
            if (whiteSpace != null && whiteSpace.getText().contains('\n')) {
                modifierList.addAfter(whiteSpace, anchor)
                whiteSpace.delete()
            }
        }
    }
}

fun removeModifier(owner: JetModifierListOwner, modifier: JetModifierKeywordToken) {
    owner.getModifierList()?.let {
        it.getModifier(modifier)?.delete()
        if (it.firstChild == null) {
            it.delete()
        }
    }
}

private val MODIFIERS_TO_REPLACE = mapOf(
        ABSTRACT_KEYWORD to listOf(OPEN_KEYWORD, FINAL_KEYWORD),
        OVERRIDE_KEYWORD to listOf(OPEN_KEYWORD),
        OPEN_KEYWORD to listOf(FINAL_KEYWORD),
        PUBLIC_KEYWORD to listOf(PROTECTED_KEYWORD, PRIVATE_KEYWORD, INTERNAL_KEYWORD),
        PROTECTED_KEYWORD to listOf(PUBLIC_KEYWORD, PRIVATE_KEYWORD, INTERNAL_KEYWORD),
        PRIVATE_KEYWORD to listOf(PUBLIC_KEYWORD, PROTECTED_KEYWORD, INTERNAL_KEYWORD),
        INTERNAL_KEYWORD to listOf(PUBLIC_KEYWORD, PROTECTED_KEYWORD, PRIVATE_KEYWORD)
)

private val MODIFIERS_ORDER = listOf(PUBLIC_KEYWORD, PROTECTED_KEYWORD, PRIVATE_KEYWORD, INTERNAL_KEYWORD,
                                     FINAL_KEYWORD, OPEN_KEYWORD, ABSTRACT_KEYWORD,
                                     OVERRIDE_KEYWORD,
                                     INNER_KEYWORD, ENUM_KEYWORD, COMPANION_KEYWORD, INFIX_KEYWORD, OPERATOR_KEYWORD)
