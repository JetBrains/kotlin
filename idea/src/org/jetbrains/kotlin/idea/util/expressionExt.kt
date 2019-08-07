/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode


fun KtCallExpression.replaceOrCreateTypeArgumentList(newTypeArgumentList: KtTypeArgumentList) {
    if (typeArgumentList != null) typeArgumentList?.replace(newTypeArgumentList)
    else addAfter(
        newTypeArgumentList,
        calleeExpression
    )
}

fun KtModifierListOwner.hasInlineModifier() = hasModifier(KtTokens.INLINE_KEYWORD)

fun KtModifierListOwner.hasPrivateModifier() = hasModifier(KtTokens.PRIVATE_KEYWORD)

fun KtPrimaryConstructor.mustHaveValOrVar(): Boolean = containingClass()?.let {
    it.isAnnotation() || it.hasInlineModifier()
} ?: false

// TODO: add cases
fun KtExpression.hasNoSideEffects(): Boolean = when (this) {
    is KtStringTemplateExpression -> !hasInterpolation()
    is KtConstantExpression -> true
    else -> ConstantExpressionEvaluator.getConstant(this, analyze(BodyResolveMode.PARTIAL)) != null
}

fun PsiElement.textRangeIn(other: PsiElement): TextRange = textRange.shiftLeft(other.startOffset)

fun KtDotQualifiedExpression.calleeTextRangeInThis(): TextRange? = callExpression?.calleeExpression?.textRangeIn(this)

fun KtNamedDeclaration.nameIdentifierTextRangeInThis(): TextRange? = nameIdentifier?.textRangeIn(this)

fun PsiElement.hasComments(): Boolean = anyDescendantOfType<PsiComment>()