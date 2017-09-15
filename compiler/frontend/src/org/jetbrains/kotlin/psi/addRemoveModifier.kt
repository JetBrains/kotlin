/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.siblings

private fun KtModifierListOwner.addModifierList(newModifierList: KtModifierList): KtModifierList {
    val anchor = firstChild!!
            .siblings(forward = true)
            .dropWhile { it is PsiComment || it is PsiWhiteSpace }
            .first()
    return addBefore(newModifierList, anchor) as KtModifierList
}

private fun createModifierList(text: String, owner: KtModifierListOwner): KtModifierList {
    return owner.addModifierList(KtPsiFactory(owner).createModifierList(text))
}

fun KtModifierListOwner.setModifierList(newModifierList: KtModifierList) {
    val currentModifierList = modifierList
    if (currentModifierList != null) {
        currentModifierList.replace(newModifierList)
    }
    else {
        addModifierList(newModifierList)
    }
}

fun addModifier(owner: KtModifierListOwner, modifier: KtModifierKeywordToken) {
    val modifierList = owner.modifierList
    if (modifierList == null) {
        createModifierList(modifier.value, owner)
    }
    else {
        addModifier(modifierList, modifier)
    }
}

fun addAnnotationEntry(owner: KtModifierListOwner, annotationEntry: KtAnnotationEntry): KtAnnotationEntry {
    val modifierList = owner.modifierList
    return if (modifierList == null) {
        createModifierList(annotationEntry.text, owner).annotationEntries.first()
    }
    else {
        modifierList.addBefore(annotationEntry, modifierList.firstChild) as KtAnnotationEntry
    }
}

internal fun addModifier(modifierList: KtModifierList, modifier: KtModifierKeywordToken) {
    if (modifierList.hasModifier(modifier)) return

    val newModifier = KtPsiFactory(modifierList).createModifier(modifier)
    val modifierToReplace = MODIFIERS_TO_REPLACE[modifier]
            ?.mapNotNull { modifierList.getModifier(it) }
            ?.firstOrNull()

    if (modifier == FINAL_KEYWORD && !modifierList.hasModifier(OVERRIDE_KEYWORD)) {
        if (modifierToReplace != null) {
            modifierToReplace.delete()
            if (modifierList.firstChild == null) {
                modifierList.delete()
            }
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
            if (child is KtAnnotation) return true // place modifiers after annotations
            val elementType = child.node!!.elementType
            val order = MODIFIERS_ORDER.indexOf(elementType)
            return newModifierOrder > order
        }

        val lastChild = modifierList.lastChild
        val anchor = lastChild?.siblings(forward = false)?.firstOrNull(::placeAfter)
        modifierList.addAfter(newModifier, anchor)

        if (anchor == lastChild) { // add line break if needed, otherwise visibility keyword may appear on previous line
            val whiteSpace = modifierList.nextSibling as? PsiWhiteSpace
            if (whiteSpace != null && whiteSpace.text.contains('\n')) {
                modifierList.addAfter(whiteSpace, anchor)
                whiteSpace.delete()
            }
        }
    }
}

fun removeModifier(owner: KtModifierListOwner, modifier: KtModifierKeywordToken) {
    owner.modifierList?.let {
        it.getModifier(modifier)?.delete()
        if (it.firstChild == null) {
            it.delete()
        }
    }
}

private val MODIFIERS_TO_REPLACE = mapOf(
        OVERRIDE_KEYWORD to listOf(OPEN_KEYWORD),
        ABSTRACT_KEYWORD to listOf(OPEN_KEYWORD, FINAL_KEYWORD),
        OPEN_KEYWORD to listOf(FINAL_KEYWORD, ABSTRACT_KEYWORD),
        FINAL_KEYWORD to listOf(ABSTRACT_KEYWORD, OPEN_KEYWORD),
        PUBLIC_KEYWORD to listOf(PROTECTED_KEYWORD, PRIVATE_KEYWORD, INTERNAL_KEYWORD),
        PROTECTED_KEYWORD to listOf(PUBLIC_KEYWORD, PRIVATE_KEYWORD, INTERNAL_KEYWORD),
        PRIVATE_KEYWORD to listOf(PUBLIC_KEYWORD, PROTECTED_KEYWORD, INTERNAL_KEYWORD),
        INTERNAL_KEYWORD to listOf(PUBLIC_KEYWORD, PROTECTED_KEYWORD, PRIVATE_KEYWORD),
        HEADER_KEYWORD to listOf(IMPL_KEYWORD, ACTUAL_KEYWORD, EXPECT_KEYWORD),
        IMPL_KEYWORD to listOf(HEADER_KEYWORD, EXPECT_KEYWORD, ACTUAL_KEYWORD),
        EXPECT_KEYWORD to listOf(IMPL_KEYWORD, ACTUAL_KEYWORD, HEADER_KEYWORD),
        ACTUAL_KEYWORD to listOf(HEADER_KEYWORD, EXPECT_KEYWORD, IMPL_KEYWORD)
)

private val MODIFIERS_ORDER = listOf(PUBLIC_KEYWORD, PROTECTED_KEYWORD, PRIVATE_KEYWORD, INTERNAL_KEYWORD,
                                     HEADER_KEYWORD, IMPL_KEYWORD, EXPECT_KEYWORD, ACTUAL_KEYWORD,
                                     FINAL_KEYWORD, OPEN_KEYWORD, ABSTRACT_KEYWORD, SEALED_KEYWORD,
                                     CONST_KEYWORD,
                                     EXTERNAL_KEYWORD,
                                     OVERRIDE_KEYWORD,
                                     LATEINIT_KEYWORD,
                                     TAILREC_KEYWORD,
                                     VARARG_KEYWORD,
                                     SUSPEND_KEYWORD,
                                     INNER_KEYWORD,
                                     ENUM_KEYWORD, ANNOTATION_KEYWORD,
                                     COMPANION_KEYWORD,
                                     INLINE_KEYWORD,
                                     INFIX_KEYWORD,
                                     OPERATOR_KEYWORD,
                                     DATA_KEYWORD)
