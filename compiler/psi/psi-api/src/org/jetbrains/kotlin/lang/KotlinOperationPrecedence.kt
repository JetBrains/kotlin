/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lang

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtTokens

enum class KotlinOperationPrecedence(val tokenSet: TokenSet) {
    POSTFIX(KtTokens.PLUSPLUS, KtTokens.MINUSMINUS, KtTokens.EXCLEXCL, KtTokens.DOT, KtTokens.SAFE_ACCESS),
    PREFIX(KtTokens.MINUS, KtTokens.PLUS, KtTokens.MINUSMINUS, KtTokens.PLUSPLUS, KtTokens.EXCL),
    TYPE_CAST(KtTokens.AS_KEYWORD, KtTokens.AS_SAFE),
    MULTIPLICATIVE(KtTokens.MUL, KtTokens.DIV, KtTokens.PERC),
    ADDITIVE(KtTokens.PLUS, KtTokens.MINUS),
    RANGE(KtTokens.RANGE, KtTokens.RANGE_UNTIL),
    SIMPLE_NAME(KtTokens.IDENTIFIER),
    ELVIS(KtTokens.ELVIS),
    IN_OR_IS(KtTokens.IN_KEYWORD, KtTokens.NOT_IN, KtTokens.IS_KEYWORD, KtTokens.NOT_IS),
    COMPARISON(KtTokens.LT, KtTokens.GT, KtTokens.LTEQ, KtTokens.GTEQ),
    EQUALITY(KtTokens.EQEQ, KtTokens.EXCLEQ, KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ),
    CONJUNCTION(KtTokens.ANDAND),
    DISJUNCTION(KtTokens.OROR),
    ASSIGNMENT(KtTokens.EQ, KtTokens.PLUSEQ, KtTokens.MINUSEQ, KtTokens.MULTEQ, KtTokens.DIVEQ, KtTokens.PERCEQ),
    ;

    constructor(operationElementType: IElementType) : this(TokenSet.create(operationElementType))
    constructor(vararg operationElementTypes: IElementType) : this(TokenSet.create(*operationElementTypes))

    val tokens: List<IElementType>
        get() = tokenSet.types.asList()

    val higher: KotlinOperationPrecedence?
        get() = entries.getOrNull(ordinal - 1)

    val lower: KotlinOperationPrecedence?
        get() = entries.getOrNull(ordinal + 1)
}