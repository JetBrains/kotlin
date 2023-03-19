/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.psi2ir.generators

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.lexer.KtTokens

internal fun getInfixOperator(ktOperator: IElementType): IrStatementOrigin? =
    when (ktOperator) {
        KtTokens.EQ -> IrStatementOrigin.EQ
        KtTokens.PLUSEQ -> IrStatementOrigin.PLUSEQ
        KtTokens.MINUSEQ -> IrStatementOrigin.MINUSEQ
        KtTokens.MULTEQ -> IrStatementOrigin.MULTEQ
        KtTokens.DIVEQ -> IrStatementOrigin.DIVEQ
        KtTokens.PERCEQ -> IrStatementOrigin.PERCEQ
        KtTokens.PLUS -> IrStatementOrigin.PLUS
        KtTokens.MINUS -> IrStatementOrigin.MINUS
        KtTokens.MUL -> IrStatementOrigin.MUL
        KtTokens.DIV -> IrStatementOrigin.DIV
        KtTokens.PERC -> IrStatementOrigin.PERC
        KtTokens.RANGE -> IrStatementOrigin.RANGE
        KtTokens.RANGE_UNTIL -> IrStatementOrigin.RANGE_UNTIL
        KtTokens.LT -> IrStatementOrigin.LT
        KtTokens.LTEQ -> IrStatementOrigin.LTEQ
        KtTokens.GT -> IrStatementOrigin.GT
        KtTokens.GTEQ -> IrStatementOrigin.GTEQ
        KtTokens.EQEQ -> IrStatementOrigin.EQEQ
        KtTokens.EXCLEQ -> IrStatementOrigin.EXCLEQ
        KtTokens.EQEQEQ -> IrStatementOrigin.EQEQEQ
        KtTokens.EXCLEQEQEQ -> IrStatementOrigin.EXCLEQEQ
        KtTokens.IN_KEYWORD -> IrStatementOrigin.IN
        KtTokens.NOT_IN -> IrStatementOrigin.NOT_IN
        KtTokens.ANDAND -> IrStatementOrigin.ANDAND
        KtTokens.OROR -> IrStatementOrigin.OROR
        KtTokens.ELVIS -> IrStatementOrigin.ELVIS
        else -> null
    }

internal fun getPrefixOperator(ktOperator: IElementType): IrStatementOrigin? =
    when (ktOperator) {
        KtTokens.PLUSPLUS -> IrStatementOrigin.PREFIX_INCR
        KtTokens.MINUSMINUS -> IrStatementOrigin.PREFIX_DECR
        KtTokens.EXCL -> IrStatementOrigin.EXCL
        KtTokens.MINUS -> IrStatementOrigin.UMINUS
        KtTokens.PLUS -> IrStatementOrigin.UPLUS
        else -> null
    }

internal fun getPostfixOperator(ktOperator: IElementType): IrStatementOrigin? =
    when (ktOperator) {
        KtTokens.PLUSPLUS -> IrStatementOrigin.POSTFIX_INCR
        KtTokens.MINUSMINUS -> IrStatementOrigin.POSTFIX_DECR
        KtTokens.EXCLEXCL -> IrStatementOrigin.EXCLEXCL
        else -> null
    }

internal fun getIrTypeOperator(ktOperator: IElementType): IrTypeOperator? =
    when (ktOperator) {
        KtTokens.IS_KEYWORD -> IrTypeOperator.INSTANCEOF
        KtTokens.NOT_IS -> IrTypeOperator.NOT_INSTANCEOF
        KtTokens.AS_KEYWORD -> IrTypeOperator.CAST
        KtTokens.AS_SAFE -> IrTypeOperator.SAFE_CAST
        else -> null
    }

internal val AUGMENTED_ASSIGNMENTS =
    setOf(IrStatementOrigin.PLUSEQ, IrStatementOrigin.MINUSEQ, IrStatementOrigin.MULTEQ, IrStatementOrigin.DIVEQ, IrStatementOrigin.PERCEQ)

internal val OPERATORS_DESUGARED_TO_CALLS =
    setOf(
        IrStatementOrigin.PLUS,
        IrStatementOrigin.MINUS,
        IrStatementOrigin.MUL,
        IrStatementOrigin.DIV,
        IrStatementOrigin.PERC,
        IrStatementOrigin.RANGE,
        IrStatementOrigin.RANGE_UNTIL,
        IrStatementOrigin.EXCL,
        IrStatementOrigin.UMINUS,
        IrStatementOrigin.UPLUS
    )

internal val COMPARISON_OPERATORS =
    setOf(IrStatementOrigin.LT, IrStatementOrigin.LTEQ, IrStatementOrigin.GT, IrStatementOrigin.GTEQ)

internal val EQUALITY_OPERATORS =
    setOf(IrStatementOrigin.EQEQ, IrStatementOrigin.EXCLEQ)

internal val IDENTITY_OPERATORS =
    setOf(IrStatementOrigin.EQEQEQ, IrStatementOrigin.EXCLEQEQ)

internal val IN_OPERATORS =
    setOf(IrStatementOrigin.IN, IrStatementOrigin.NOT_IN)

internal val BINARY_BOOLEAN_OPERATORS =
    setOf(IrStatementOrigin.ANDAND, IrStatementOrigin.OROR)

internal val INCREMENT_DECREMENT_OPERATORS =
    setOf(IrStatementOrigin.PREFIX_INCR, IrStatementOrigin.PREFIX_DECR, IrStatementOrigin.POSTFIX_INCR, IrStatementOrigin.POSTFIX_DECR)
