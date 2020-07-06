/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir.modifier

import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtTokens.*

object ModifierSets {
    val CLASS_MODIFIER = TokenSet.create(
        ENUM_KEYWORD,
        ANNOTATION_KEYWORD,
        DATA_KEYWORD,
        INNER_KEYWORD,
        COMPANION_KEYWORD,
        FUN_KEYWORD
    )
    val MEMBER_MODIFIER = TokenSet.create(OVERRIDE_KEYWORD, LATEINIT_KEYWORD)
    val VISIBILITY_MODIFIER = TokenSet.create(
        PUBLIC_KEYWORD,
        PRIVATE_KEYWORD,
        INTERNAL_KEYWORD,
        PROTECTED_KEYWORD
    )
    val FUNCTION_MODIFIER = TokenSet.create(
        TAILREC_KEYWORD,
        OPERATOR_KEYWORD,
        INFIX_KEYWORD,
        EXTERNAL_KEYWORD,
        SUSPEND_KEYWORD
    )
    val PROPERTY_MODIFIER = TokenSet.create(CONST_KEYWORD)
    val INHERITANCE_MODIFIER = TokenSet.create(
        ABSTRACT_KEYWORD,
        FINAL_KEYWORD,
        OPEN_KEYWORD,
        SEALED_KEYWORD
    )
    val PARAMETER_MODIFIER = TokenSet.create(VARARG_KEYWORD, NOINLINE_KEYWORD, CROSSINLINE_KEYWORD)
    val PLATFORM_MODIFIER = TokenSet.create(EXPECT_KEYWORD, ACTUAL_KEYWORD, HEADER_KEYWORD, IMPL_KEYWORD)
    val VARIANCE_MODIFIER = TokenSet.create(IN_KEYWORD, OUT_KEYWORD)
    val REIFICATION_MODIFIER = TokenSet.create(REIFIED_KEYWORD)
    val INLINE_MODIFIER = TokenSet.create(INLINE_KEYWORD)
}