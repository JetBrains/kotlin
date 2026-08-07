/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.isClassWithFqName
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.wasm.config.wasmEnableTailCalls

val WASM_TAIL_CALL by IrStatementOriginImpl

/**
 * Marks every [IrCall] in tail position with [WASM_TAIL_CALL] origin so that
 * [BodyGenerator][org.jetbrains.kotlin.backend.wasm.ir2wasm.codegenGenerators.BodyGenerator]
 * can emit `return_call` / `return_call_ref` without re-analysing the IR.
 *
 * Must run after all lowerings that may wrap calls in blocks, try-catch, or
 * continuation machinery, so that the structural tail-position analysis sees
 * the final IR shape. Placed at the very end of the Wasm lowering pipeline.
 */
internal class WasmTailCallLowering(private val context: WasmBackendContext) : BodyLoweringPass {
    private val enabled = context.configuration.wasmEnableTailCalls

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (!enabled) return
        val irFunction = container as? IrFunction ?: return
        if (irFunction is IrConstructor) return
        markTailCalls(irFunction)
    }
}

private fun markTailCalls(irFunction: IrFunction) {
    val isUnitReturn = irFunction.returnType.isUnit()

    val visitor = object : IrVisitor<Unit, Boolean>() {
        override fun visitElement(element: IrElement, data: Boolean) {
            element.acceptChildren(this, false)
        }

        override fun visitFunction(declaration: IrFunction, data: Boolean) {}
        override fun visitClass(declaration: IrClass, data: Boolean) {}
        override fun visitTry(aTry: IrTry, data: Boolean) {}

        override fun visitReturn(expression: IrReturn, data: Boolean) {
            val isTail = expression.returnTargetSymbol == irFunction.symbol
            expression.value.accept(this, isTail)
        }

        override fun visitExpressionBody(body: IrExpressionBody, data: Boolean) =
            body.acceptChildren(this, data)

        override fun visitBlockBody(body: IrBlockBody, data: Boolean) =
            visitStatementContainer(body, data)

        override fun visitContainerExpression(expression: IrContainerExpression, data: Boolean) =
            visitStatementContainer(expression, data)

        private fun visitStatementContainer(container: IrStatementContainer, data: Boolean) {
            container.statements.forEachIndexed { index, irStatement ->
                val isTailStatement = if (index == container.statements.lastIndex) {
                    data
                } else {
                    isUnitReturn && container.statements[index + 1].let {
                        it is IrReturn && it.returnTargetSymbol == irFunction.symbol && it.value.isUnitRead()
                    }
                }
                irStatement.accept(this, isTailStatement)
            }
        }

        private fun IrExpression.isUnitRead(): Boolean =
            this is IrGetObjectValue && symbol.isClassWithFqName(StandardNames.FqNames.unit)

        override fun visitWhen(expression: IrWhen, data: Boolean) {
            expression.branches.forEach {
                it.condition.accept(this, false)
                it.result.accept(this, data)
            }
        }

        override fun visitCall(expression: IrCall, data: Boolean) {
            expression.acceptChildren(this, false)
            if (data) {
                expression.origin = WASM_TAIL_CALL
            }
        }
    }

    irFunction.body?.accept(visitor, true)
}
