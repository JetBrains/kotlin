/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irComposite
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol

/**
 * Wraps returnable blocks with returns to composite and replaces returns with assignment to temporary variable + `return Unit`,
 * also, it changes type of returnable block to Unit.
 *
 * ```
 * returnable_block {
 *   ...
 *   return@returnable_block e
 *   ...
 * }: T
 * ```
 *
 * is transformed into
 *
 * ```
 * composite {
 *   val result
 *   returnable_block {
 *     ...
 *     result = e
 *     return@returnable_block Unit
 *     ...
 *   }: Unit
 *   result
 * }: T
 * ```
 */
class JsReturnableBlockLowering(val context: CommonBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        container.transform(JsReturnableBlockTransformer(context), null)
    }
}

class JsReturnableBlockTransformer(val context: CommonBackendContext) : IrElementTransformerVoidWithContext() {
    private var labelCnt = 0
    private var map = hashMapOf<IrReturnableBlockSymbol, IrVariable>()

    private val unitType = context.irBuiltIns.unitType
    private val unitValue get() = IrGetObjectValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, unitType, context.irBuiltIns.unitClass)

    private inline fun withNewVariableForReturnableBlock(
        returnableBlock: IrReturnableBlock,
        action: (IrVariable) -> IrExpression,
    ): IrExpression {
        val variable = currentScope!!.scope.createTemporaryVariable(
            irExpression = returnableBlock,
            nameHint = "tmp\$ret\$${labelCnt++}",
            isMutable = true,
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            inventUniqueName = false,
        ).apply {
            initializer = null
        }

        val previousVariable = map.put(returnableBlock.symbol, variable)
        check(previousVariable == null) { "Returnable block is already mapped to another variable: $previousVariable" }

        val result = action(variable)

        val removedVariable = map.remove(returnableBlock.symbol)
        check(removedVariable == variable) { "Removed incorrect variable: $removedVariable, expected: $variable" }

        return result
    }

    override fun visitBlock(expression: IrBlock): IrExpression {
        if (expression !is IrReturnableBlock) return super.visitBlock(expression)

        return withNewVariableForReturnableBlock(returnableBlock = expression) { variable ->
            expression.transformChildrenVoid()

            val builder = context.createIrBuilder(expression.symbol)

            expression.type = unitType
            return builder.irComposite(expression, expression.origin, variable.type) {
                +variable
                +expression
                +irGet(variable)
            }
        }
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        expression.transformChildrenVoid()

        val targetSymbol = expression.returnTargetSymbol
        if (targetSymbol !is IrReturnableBlockSymbol) return expression

        val variable = map[targetSymbol] ?: compilationException("Unmapped returnable block $targetSymbol", expression)

        val builder = context.createIrBuilder(targetSymbol)
        return builder.at(UNDEFINED_OFFSET, UNDEFINED_OFFSET).irComposite {
            +at(expression).irSet(variable.symbol, expression.value)
            +at(UNDEFINED_OFFSET, UNDEFINED_OFFSET).irReturn(unitValue)
        }
    }
}
