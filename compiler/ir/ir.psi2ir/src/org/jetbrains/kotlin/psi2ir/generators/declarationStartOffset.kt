/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.psi2ir.getChildTokenStartOffsetOrNull

private val FUNCTION_DECL_TOKENS = TokenSet.create(KtTokens.FUN_KEYWORD)
private val ACCESSOR_DECL_TOKENS = TokenSet.create(KtTokens.GET_KEYWORD, KtTokens.SET_KEYWORD)
private val PROPERTY_DECL_TOKENS = TokenSet.create(KtTokens.VAL_KEYWORD, KtTokens.VAR_KEYWORD)
private val CLASS_DECL_TOKENS = TokenSet.create(KtTokens.CLASS_KEYWORD, KtTokens.INTERFACE_KEYWORD)
private val CONSTRUCTOR_DECL_TOKENS = TokenSet.create(KtTokens.CONSTRUCTOR_KEYWORD)

internal fun KtPureElement.getStartOffsetOfFunctionDeclarationKeywordOrNull(): Int? =
    when (this) {
        is KtNamedFunction -> getChildTokenStartOffsetOrNull(FUNCTION_DECL_TOKENS)
        is KtPropertyAccessor -> getChildTokenStartOffsetOrNull(ACCESSOR_DECL_TOKENS)
        is KtProperty -> getChildTokenStartOffsetOrNull(PROPERTY_DECL_TOKENS)
        is KtParameter -> getChildTokenStartOffsetOrNull(PROPERTY_DECL_TOKENS)
        is KtClassOrObject -> getChildTokenStartOffsetOrNull(CLASS_DECL_TOKENS)
        else -> null
    }

internal fun KtPureClassOrObject.getStartOffsetOfClassDeclarationOrNull(): Int? =
    when (this) {
        is KtClassOrObject -> startOffsetSkippingComments
        else -> null
    }

internal fun KtPureElement.getStartOffsetOfConstructorDeclarationKeywordOrNull(): Int? =
    when (this) {
        is KtPrimaryConstructor -> getChildTokenStartOffsetOrNull(CONSTRUCTOR_DECL_TOKENS)

        is KtSecondaryConstructor -> getChildTokenStartOffsetOrNull(CONSTRUCTOR_DECL_TOKENS)

        is KtClassOrObject ->
            primaryConstructor?.getStartOffsetOfConstructorDeclarationKeywordOrNull()
                ?: getChildTokenStartOffsetOrNull(CONSTRUCTOR_DECL_TOKENS)
                ?: getChildTokenStartOffsetOrNull(CLASS_DECL_TOKENS)

        else -> null
    }