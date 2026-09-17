/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ir

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.OperatorNames
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.isFalseConst
import org.jetbrains.kotlin.ir.util.isTrueConst
import org.jetbrains.kotlin.name.Name

class JsIrArithBuilder(val context: JsIrBackendContext) {

    val symbols = context.symbols

    private fun buildBinaryOperator(name: Name, l: IrExpression, r: IrExpression): IrExpression {
        val symbol = context.getOperatorByName(name, l.type as IrSimpleType, r.type as IrSimpleType)
        return JsIrBuilder.buildCall(symbol!!).apply {
            arguments[0] = l
            arguments[1] = r
        }
    }

    private fun buildUnaryOperator(name: Name, v: IrExpression): IrExpression {
        val symbol = context.getOperatorByName(name, v.type as IrSimpleType, null)!!
        return JsIrBuilder.buildCall(symbol).apply { dispatchReceiver = v }
    }

    fun add(l: IrExpression, r: IrExpression) = buildBinaryOperator(OperatorNames.ADD, l, r)
    fun sub(l: IrExpression, r: IrExpression) = buildBinaryOperator(OperatorNames.SUB, l, r)
    fun mul(l: IrExpression, r: IrExpression) = buildBinaryOperator(OperatorNames.MUL, l, r)
    fun div(l: IrExpression, r: IrExpression) = buildBinaryOperator(OperatorNames.DIV, l, r)
    fun rem(l: IrExpression, r: IrExpression) = buildBinaryOperator(OperatorNames.REM, l, r)
    fun and(l: IrExpression, r: IrExpression) = buildBinaryOperator(OperatorNames.AND, l, r)
    fun or(l: IrExpression, r: IrExpression) = buildBinaryOperator(OperatorNames.OR, l, r)
    fun shl(l: IrExpression, r: IrExpression) = buildBinaryOperator(OperatorNames.SHL, l, r)
    fun shr(l: IrExpression, r: IrExpression) = buildBinaryOperator(OperatorNames.SHR, l, r)
    fun shru(l: IrExpression, r: IrExpression) = buildBinaryOperator(OperatorNames.SHRU, l, r)
    fun not(v: IrExpression): IrExpression = buildUnaryOperator(OperatorNames.NOT, v)
    fun inv(v: IrExpression): IrExpression = buildUnaryOperator(OperatorNames.INV, v)

    /**
     * The rules of picking `l` and `r` in case of a boolean constant for one of the arguments:
     * ```
     *               ┌───────────────────────────┬───────────────────────────┐
     *               │        l is const         │        r is const         │
     *               ├─────────────┬─────────────┼─────────────┬─────────────┤
     *               │    true     │    false    │    true     │    false    │
     * ┌─────────────╋━━━━━━━━━━━━━┿━━━━━━━━━━━━━┿━━━━━━━━━━━━━┿━━━━━━━━━━━━━┫
     * │   andand    ┃      r      │    false    │      l      │    false    │
     * └─────────────┻─────────────┴─────────────┴─────────────┴─────────────┘
     * ```
     */
    fun andand(l: IrExpression, r: IrExpression) =
        when {
            l.isTrueConst() -> r
            l.isFalseConst() -> l
            r.isTrueConst() -> l
            r.isFalseConst() -> r
            else -> JsIrBuilder.buildIfElse( // if (l) r else false
                type = context.irBuiltIns.booleanType,
                cond = l,
                thenBranch = r,
                elseBranch = JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, false),
                origin = IrStatementOrigin.ANDAND
            )
        }

    /**
     * The rules of picking `l` and `r` in case of a boolean constant for one of the arguments:
     * ```
     *               ┌───────────────────────────┬───────────────────────────┐
     *               │        l is const         │        r is const         │
     *               ├─────────────┬─────────────┼─────────────┬─────────────┤
     *               │    true     │    false    │    true     │    false    │
     * ┌─────────────╋━━━━━━━━━━━━━┿━━━━━━━━━━━━━┿━━━━━━━━━━━━━┿━━━━━━━━━━━━━┫
     * │    oror     ┃    true     │      r      │    true     │      l      │
     * └─────────────┻─────────────┴─────────────┴─────────────┴─────────────┘
     * ```
     */
    fun oror(l: IrExpression, r: IrExpression) =
        when {
            l.isTrueConst() -> l
            l.isFalseConst() -> r
            r.isFalseConst() -> r
            r.isTrueConst() -> r
            else -> JsIrBuilder.buildIfElse( // if (l) true else r
                type = context.irBuiltIns.booleanType,
                cond = l,
                thenBranch = JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, true),
                elseBranch = r,
                origin = IrStatementOrigin.OROR
            )
        }
}
