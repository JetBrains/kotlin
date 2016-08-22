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
import org.jetbrains.kotlin.ir.expressions.IrOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.lexer.KtTokens


fun getInfixOperator(ktOperator: IElementType): IrOperator? =
        when (ktOperator) {
            KtTokens.EQ -> IrOperator.EQ
            KtTokens.PLUSEQ -> IrOperator.PLUSEQ
            KtTokens.MINUSEQ -> IrOperator.MINUSEQ
            KtTokens.MULTEQ -> IrOperator.MULTEQ
            KtTokens.DIVEQ -> IrOperator.DIVEQ
            KtTokens.PERCEQ -> IrOperator.PERCEQ
            KtTokens.PLUS -> IrOperator.PLUS
            KtTokens.MINUS -> IrOperator.MINUS
            KtTokens.MUL -> IrOperator.MUL
            KtTokens.DIV -> IrOperator.DIV
            KtTokens.PERC -> IrOperator.PERC
            KtTokens.RANGE -> IrOperator.RANGE
            KtTokens.LT -> IrOperator.LT
            KtTokens.LTEQ -> IrOperator.LTEQ
            KtTokens.GT -> IrOperator.GT
            KtTokens.GTEQ -> IrOperator.GTEQ
            KtTokens.EQEQ -> IrOperator.EQEQ
            KtTokens.EXCLEQ -> IrOperator.EXCLEQ
            KtTokens.EQEQEQ -> IrOperator.EQEQEQ
            KtTokens.EXCLEQEQEQ -> IrOperator.EXCLEQEQ
            KtTokens.IN_KEYWORD -> IrOperator.IN
            KtTokens.NOT_IN -> IrOperator.NOT_IN
            KtTokens.ANDAND -> IrOperator.ANDAND
            KtTokens.OROR -> IrOperator.OROR
            KtTokens.ELVIS -> IrOperator.ELVIS
            else -> null
        }

fun getPrefixOperator(ktOperator: IElementType): IrOperator? =
        when (ktOperator) {
            KtTokens.PLUSPLUS -> IrOperator.PREFIX_INCR
            KtTokens.MINUSMINUS -> IrOperator.PREFIX_DECR
            KtTokens.EXCL -> IrOperator.EXCL
            KtTokens.MINUS -> IrOperator.UMINUS
            KtTokens.PLUS -> IrOperator.UPLUS
            else -> null
        }

fun getPostfixOperator(ktOperator: IElementType): IrOperator? =
        when (ktOperator) {
            KtTokens.PLUSPLUS -> IrOperator.POSTFIX_INCR
            KtTokens.MINUSMINUS -> IrOperator.POSTFIX_DECR
            KtTokens.EXCLEXCL -> IrOperator.EXCLEXCL
            else -> null
        }

fun getIrTypeOperator(ktOperator: IElementType): IrTypeOperator? =
        when (ktOperator) {
            KtTokens.IS_KEYWORD -> IrTypeOperator.INSTANCEOF
            KtTokens.NOT_IS -> IrTypeOperator.NOT_INSTANCEOF
            KtTokens.AS_KEYWORD -> IrTypeOperator.CAST
            KtTokens.AS_SAFE -> IrTypeOperator.SAFE_CAST
            else -> null
        }

val AUGMENTED_ASSIGNMENTS =
        setOf(IrOperator.PLUSEQ, IrOperator.MINUSEQ, IrOperator.MULTEQ, IrOperator.DIVEQ, IrOperator.PERCEQ)

val OPERATORS_DESUGARED_TO_CALLS =
        setOf(IrOperator.PLUS, IrOperator.MINUS, IrOperator.MUL, IrOperator.DIV, IrOperator.PERC, IrOperator.RANGE,
              IrOperator.EXCL, IrOperator.UMINUS, IrOperator.UPLUS)

val COMPARISON_OPERATORS =
        setOf(IrOperator.LT, IrOperator.LTEQ, IrOperator.GT, IrOperator.GTEQ)

val EQUALITY_OPERATORS =
        setOf(IrOperator.EQEQ, IrOperator.EXCLEQ)

val IDENTITY_OPERATORS =
        setOf(IrOperator.EQEQEQ, IrOperator.EXCLEQEQ)

val IN_OPERATORS =
        setOf(IrOperator.IN, IrOperator.NOT_IN)

val BINARY_BOOLEAN_OPERATORS =
        setOf(IrOperator.ANDAND, IrOperator.OROR)

val INCREMENT_DECREMENT_OPERATORS =
        setOf(IrOperator.PREFIX_INCR, IrOperator.PREFIX_DECR, IrOperator.POSTFIX_INCR, IrOperator.POSTFIX_DECR)

val POSTFIX_INCREMENT_DECREMENT_OPERATORS =
        setOf(IrOperator.POSTFIX_INCR, IrOperator.POSTFIX_DECR)