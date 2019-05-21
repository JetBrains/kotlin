/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.psiUtil.containingClass


fun KtCallExpression.replaceOrCreateTypeArgumentList(newTypeArgumentList: KtTypeArgumentList) {
    if (typeArgumentList != null) typeArgumentList?.replace(newTypeArgumentList)
    else addAfter(
        newTypeArgumentList,
        calleeExpression
    )
}

fun KtModifierListOwner.hasInlineModifier() = hasModifier(KtTokens.INLINE_KEYWORD)

fun KtPrimaryConstructor.allowedValOrVar(): Boolean = containingClass()?.let {
    it.isAnnotation() || it.hasInlineModifier()
} ?: false