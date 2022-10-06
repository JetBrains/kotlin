/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.isFalseConst
import org.jetbrains.kotlin.ir.util.isTrueConst

class RemoveUnreachableStatementsLowering(context: JsIrBackendContext) : BodyLoweringPass {
    private val transformer = RemoveUnreachableStatementsLoweringVisitor(context)

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(transformer)
    }
}

private class RemoveUnreachableStatementsLoweringVisitor(val context: JsIrBackendContext) : IrElementTransformerVoidWithContext() {
    private val unitType = context.irBuiltIns.unitType
    private val unitValue get() = IrGetObjectValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, unitType, context.irBuiltIns.unitClass)

    override fun visitWhileLoop(loop: IrWhileLoop): IrExpression {
        return if (loop.condition.isFalseConst()) {
            unitValue
        } else {
            super.visitWhileLoop(loop)
        }
    }

    override fun visitWhen(expression: IrWhen): IrExpression {
        var i = 0;
        val branches = expression.branches
        while (i < branches.size) {
            val it = branches[i]

            if (it.condition.isFalseConst()) {
                branches.removeAt(i)
            } else if (it.condition.isTrueConst() && (i != branches.lastIndex || branches.size == 1) ) {
                return super.visitExpression(it.result)
            } else {
                i++
            }
        }

        return if (expression.branches.isEmpty()) {
            unitValue
        } else {
            super.visitWhen(expression)
        }
    }
}