/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Transforms `assert(...) {...}` call into
 *
 * ```
 * if (isAssertionArgumentEvaluationEnabled) {
 *     assert(...) {...}
 * }
 * ```
 */
abstract class KlibAssertionWrapperLowering(val context: CommonBackendContext) : FileLoweringPass {
    private val asserts = context.ir.symbols.asserts.toSet()

    protected abstract val isAssertionArgumentEvaluationEnabled: IrSimpleFunctionSymbol

    fun lower(function: IrFunction) {
        function.transformChildren(Transformer(), function.symbol)
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildren(Transformer(), irFile.symbol)
    }

    private inner class Transformer : IrElementTransformer<IrSymbol> {
        override fun visitElement(element: IrElement, data: IrSymbol): IrElement {
            return super.visitElement(element, if (element is IrSymbolOwner) element.symbol else data)
        }

        override fun visitDeclaration(declaration: IrDeclarationBase, data: IrSymbol): IrStatement {
            return super.visitDeclaration(declaration, declaration.symbol)
        }

        override fun visitCall(expression: IrCall, data: IrSymbol): IrElement {
            if (expression.symbol !in asserts) return super.visitCall(expression, data)

            val builder = context.createIrBuilder(data, expression.startOffset, expression.endOffset)
            return builder.irIfThen(builder.irCall(isAssertionArgumentEvaluationEnabled), expression)
        }
    }
}
