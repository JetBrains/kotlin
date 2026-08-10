/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtNonPublicApi::class)

package org.jetbrains.kotlin.psi.addRemoveModifier

import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.*

@Deprecated(
    message = "Use setModifierList(newModifierList) instead",
    replaceWith = ReplaceWith("this.setModifierList(newModifierList)", "org.jetbrains.kotlin.idea.base.psi.setModifierList"),
)
fun KtModifierListOwner.setModifierList(newModifierList: KtModifierList) {
    KtPsiMutationService.getInstance().setModifierList(this, newModifierList)
}

@Deprecated(
    message = "Use owner.addModifierKeyword(modifier) instead",
    replaceWith = ReplaceWith("owner.addModifierKeyword(modifier)", "org.jetbrains.kotlin.idea.base.psi.addModifierKeyword"),
)
fun addModifier(owner: KtModifierListOwner, modifier: KtModifierKeywordToken) {
    KtPsiMutationService.getInstance().addModifierKeyword(owner, modifier)
}

@Deprecated(
    message = "Use owner.addAnnotation(annotationEntry) instead",
    replaceWith = ReplaceWith("owner.addAnnotation(annotationEntry)", "org.jetbrains.kotlin.idea.base.psi.addAnnotation"),
)
fun addAnnotationEntry(owner: KtModifierListOwner, annotationEntry: KtAnnotationEntry): KtAnnotationEntry =
    KtPsiMutationService.getInstance().addAnnotation(owner, annotationEntry)

@Deprecated(
    message = "Use owner.removeModifierKeyword(modifier) instead",
    replaceWith = ReplaceWith("owner.removeModifierKeyword(modifier)", "org.jetbrains.kotlin.idea.base.psi.removeModifierKeyword"),
)
fun removeModifier(owner: KtModifierListOwner, modifier: KtModifierKeywordToken) {
    KtPsiMutationService.getInstance().removeModifierKeyword(owner, modifier)
}

fun sortModifiers(modifiers: List<KtModifierKeywordToken>): List<KtModifierKeywordToken> {
    return modifiers.sortedBy {
        val index = MODIFIER_KEYWORDS_ARRAY.indexOf(it)
        if (index == -1) Int.MAX_VALUE else index
    }
}

@Deprecated(
    message = "Use `KtTokens.MODIFIER_KEYWORDS_ARRAY` directly",
    replaceWith = ReplaceWith("KtTokens.MODIFIER_KEYWORDS_ARRAY", "org.jetbrains.kotlin.lexer.KtTokens"),
)
val MODIFIERS_ORDER: List<KtModifierKeywordToken> get() = MODIFIER_KEYWORDS_ARRAY.asList()
