/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class SequenceFusionLowering(val context: CommonBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val transformer = SequenceFusionTransformer(context)
        irFile.transformChildrenVoid(transformer)
    }
}

private class SequenceFusionTransformer(val context: CommonBackendContext) : IrElementTransformerVoid() {
    private val sequences: MutableSet<IrElement> = mutableSetOf()
    override fun visitCall(expression: IrCall): IrExpression {
        val sequenceSymbol = context.symbols.sequence ?: return super.visitCall(expression)
        if (!expression.type.isSubtypeOfClass(sequenceSymbol)) return super.visitCall(expression)
        sequences.add(expression)
        return super.visitCall(expression)
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        val sequenceSymbol = context.symbols.sequence ?: return super.visitVariable(declaration)
        if (!declaration.type.isSubtypeOfClass(sequenceSymbol)) return super.visitVariable(declaration)
        sequences.add(declaration)
        return super.visitVariable(declaration)
    }

    override fun visitExpression(expression: IrExpression): IrExpression {
        val sequenceSymbol = context.symbols.sequence ?: return super.visitExpression(expression)
        if (!expression.type.isSubtypeOfClass(sequenceSymbol)) return super.visitExpression(expression)
        sequences.add(expression)
        return super.visitExpression(expression)
    }
}
