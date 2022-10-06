/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.backend.js.utils.isTheLastReturnStatementIn

class CompositeToBlockLowering : BodyLoweringPass {
    private val transformer = CompositeToBlockLoweringVisitor()

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(transformer)
    }
}

private class CompositeToBlockLoweringVisitor : IrElementTransformerVoid() {
    private val containsSelfReturn = mutableSetOf<IrReturnableBlockSymbol>();

    override fun visitComposite(expression: IrComposite): IrExpression {
        return IrBlockImpl(expression.startOffset, expression.endOffset, expression.type, expression.origin, expression.statements)
    }

    override fun visitBlock(expression: IrBlock): IrExpression {
        val result = super.visitBlock(expression)

        if (expression is IrReturnableBlock && expression.symbol !in containsSelfReturn) {
            return IrBlockImpl(expression.startOffset, expression.endOffset, expression.type, expression.origin, expression.statements)
        }

        return result
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        val targetSymbol = expression.returnTargetSymbol
        if (targetSymbol is IrReturnableBlockSymbol && !expression.isTheLastReturnStatementIn(targetSymbol)) {
            containsSelfReturn.add(targetSymbol)
        }

        return super.visitReturn(expression)
    }
}