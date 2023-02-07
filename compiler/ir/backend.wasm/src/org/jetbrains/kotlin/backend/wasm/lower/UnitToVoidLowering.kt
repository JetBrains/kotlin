/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.AbstractValueUsageTransformer
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.web.JsStatementOrigins
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

// NOTE: This is an optional lowering.
// In the wasm backend we treat Unit type as a regular object (except for the function return values). This means that all expressions
// with Unit return type will actually produce a real Unit instance. However sometimes Unit type signifies that the result of the
// expression will never be used and for such cases we want to be able to avoid generating redundant Unit objects.
// In order to simplify the reasoning we introduce special type 'Void'. It means absence of any value, so there are never any real
// objects of type Void. The only way to produce a Void type is by calling intrinsic 'consumeAnyIntoVoid'. In the wasm code it will turn
// into a single drop instruction.
// This lowering visits all statements with Unit return type and tries to replace it with Void type. In the result of
// such statements it emits 'consumeAnyIntoVoid(previous_result)'. In other words it tries to propagate 'consumeAnyIntoVoid' as
// deep as possible, changing statement return type to Void on each nesting level it crosses. For example, from this code:
//    block_body {
//        block: Unit {
//            if (some_condition): Unit {
//                foo(): Unit
//            } else {
//                IMPLICIT_COERCION_TO_UNIT(bar(): Int)
//            }
//            foo.bar = 10
//        }
//        return 1
//    }
// We will get this code:
//    block_body {
//        block: Void {
//            if (some_condition): Void {
//                consumeAnyIntoVoid(foo(): Unit): Void
//            } else {
//                consumeAnyIntoVoid(bar(): Int): Void
//            }
//            foo.bar = 10
//        }
//        return 1
//    }
// NOTE: In order to reduce the amount of new nodes in the IR we only do this transformation if it involves 'when' or 'try/catch' statements
// because WASM backend handles other cases well enough.
// As a further optimization we can directly mark 'call', 'set_value' and 'set_field' with Void return type and handle them as special
// cases in the backend.

class UnitToVoidLowering(val context: WasmBackendContext) : FileLoweringPass, AbstractValueUsageTransformer(context.irBuiltIns) {
    val builtIns = context.irBuiltIns
    val symbols = context.wasmSymbols

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun IrExpression.useAsStatement(): IrExpression {
        if (type != builtIns.unitType)
            return this

        if (shouldVoidify(this))
            return voidify(this) as IrExpression
        return this
    }

    // Checks if this expression is an interesting target for the voidification.
    private fun shouldVoidify(expr: IrStatement): Boolean {
        if (expr !is IrExpression)
            return true

        if (expr.type == symbols.voidType || expr.type == builtIns.nothingType)
            return false

        return when (expr) {
            is IrContainerExpression ->
                expr.statements.isNotEmpty() && expr !is IrReturnableBlock && shouldVoidify(expr.statements.last())

            is IrWhen, is IrTry ->
                true

            is IrTypeOperatorCall ->
                expr.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT

            else -> false
        }
    }

    // Takes expression of any type and unconditionally turns it into an expression of a void type.
    private fun voidify(expr: IrStatement): IrStatement {
        // Declarations are implicitly void
        if (expr !is IrExpression)
            return expr

        // Don't voidify 'Nothing' because it can be casted to anything, including void.
        if (expr.type == symbols.voidType || expr.type == builtIns.nothingType)
            return expr

        // Try to look through one of the known expression types.
        when (expr) {
            is IrContainerExpression -> {
                expr.type = symbols.voidType
                expr.statements.lastOrNull()?.let { last ->
                    expr.statements[expr.statements.lastIndex] = voidify(last)
                }
                return expr
            }
            is IrWhen -> {
                expr.type = symbols.voidType
                expr.branches.forEachIndexed { i, branch ->
                    expr.branches[i].result = voidify(branch.result) as IrExpression
                }
                return expr
            }
            is IrTry -> {
                expr.type = symbols.voidType
                expr.tryResult = voidify(expr.tryResult) as IrExpression
                expr.catches.forEachIndexed { i, catch ->
                    expr.catches[i].result = voidify(catch.result) as IrExpression
                }
                return expr
            }
            is IrTypeOperatorCall -> {
                if (expr.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT)
                    return voidify(expr.argument)
            }
        }

        return IrCallImpl(expr.startOffset, expr.endOffset, symbols.voidType, symbols.findVoidConsumer(expr.type), 0, 1).apply {
            putValueArgument(0, expr)
        }
    }
}
