/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the licensedot/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.resolve.KeywordType

fun getKeywordType(modifier: FirModifier<*>): KeywordType {
    return ktKeywordToKeywordTypeMap[modifier.token]!!
}

private val ktKeywordToKeywordTypeMap: Map<KtKeywordToken, KeywordType> = mapOf(
    INNER_KEYWORD to KeywordType.Inner,
    OVERRIDE_KEYWORD to KeywordType.Override,
    PUBLIC_KEYWORD to KeywordType.Public,
    PROTECTED_KEYWORD to KeywordType.Protected,
    INTERNAL_KEYWORD to KeywordType.Internal,
    PRIVATE_KEYWORD to KeywordType.Private,
    COMPANION_KEYWORD to KeywordType.Companion,
    FINAL_KEYWORD to KeywordType.Final,
    VARARG_KEYWORD to KeywordType.Vararg,
    ENUM_KEYWORD to KeywordType.Enum,
    ABSTRACT_KEYWORD to KeywordType.Abstract,
    OPEN_KEYWORD to KeywordType.Open,
    SEALED_KEYWORD to KeywordType.Sealed,
    IN_KEYWORD to KeywordType.In,
    OUT_KEYWORD to KeywordType.Out,
    REIFIED_KEYWORD to KeywordType.Reified,
    LATEINIT_KEYWORD to KeywordType.Lateinit,
    DATA_KEYWORD to KeywordType.Data,
    INLINE_KEYWORD to KeywordType.Inline,
    NOINLINE_KEYWORD to KeywordType.Noinline,
    TAILREC_KEYWORD to KeywordType.Tailrec,
    SUSPEND_KEYWORD to KeywordType.Suspend,
    EXTERNAL_KEYWORD to KeywordType.External,
    ANNOTATION_KEYWORD to KeywordType.Annotation,
    CROSSINLINE_KEYWORD to KeywordType.Crossinline,
    CONST_KEYWORD to KeywordType.Const,
    OPERATOR_KEYWORD to KeywordType.Operator,
    INFIX_KEYWORD to KeywordType.Infix,
    HEADER_KEYWORD to KeywordType.Header,
    IMPL_KEYWORD to KeywordType.Impl,
    EXPECT_KEYWORD to KeywordType.Expect,
    ACTUAL_KEYWORD to KeywordType.Actual,
    FUN_KEYWORD to KeywordType.Fun,
    VALUE_KEYWORD to KeywordType.Value
)