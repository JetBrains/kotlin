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

import org.jetbrains.kotlin.lexer.JetModifierKeywordToken
import org.jetbrains.kotlin.lexer.JetTokens.*
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.JetModifierListOwner
import org.jetbrains.kotlin.psi.JetPsiFactory
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.JetAnnotation
import org.jetbrains.kotlin.psi.JetModifierList
import org.jetbrains.kotlin.psi.psiUtil.siblings

internal fun addModifier(owner: JetModifierListOwner, modifier: JetModifierKeywordToken, defaultVisibilityModifier: JetModifierKeywordToken) {
    val modifierList = owner.getModifierList()
    if (modifierList == null) {
        if (modifier == defaultVisibilityModifier) return

        val newModifierList = JetPsiFactory(owner).createModifierList(modifier)
        val anchor = owner.getFirstChild()!!.siblings(forward = true)
                .dropWhile { it is PsiComment || it is PsiWhiteSpace }
                .first()
        owner.addBefore(newModifierList, anchor)
    }
    else {
        addModifier(modifierList, modifier, defaultVisibilityModifier)
    }
}

internal fun addModifier(modifierList: JetModifierList, modifier: JetModifierKeywordToken, defaultVisibilityModifier: JetModifierKeywordToken) {
    val newModifier = JetPsiFactory(modifierList).createModifierList(modifier).getModifier(modifier)!!
    val modifierToReplace = MODIFIERS_TO_REPLACE[modifier]
            ?.map { modifierList.getModifier(it) }
            ?.filterNotNull()
            ?.firstOrNull()

    if (modifier == defaultVisibilityModifier) { // do not insert explicit 'internal' keyword (or 'public' for primary constructor)
        //TODO: code style option
        if (modifierToReplace != null) {
            modifierToReplace.delete()
        }
        return
    }

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

        val anchor = modifierList.getLastChild()?.siblings(forward = false)?.firstOrNull(::placeAfter)
        modifierList.addAfter(newModifier, anchor)
    }
}

internal fun removeModifier(owner: JetModifierListOwner, modifier: JetModifierKeywordToken) {
    owner.getModifierList()?.getModifier(modifier)?.delete()
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
                                     INNER_KEYWORD,
                                     ANNOTATION_KEYWORD, ENUM_KEYWORD, DEFAULT_KEYWORD)
