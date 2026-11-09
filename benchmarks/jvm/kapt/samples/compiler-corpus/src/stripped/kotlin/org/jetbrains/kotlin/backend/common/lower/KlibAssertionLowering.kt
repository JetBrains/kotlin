/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ir.PreSerializationNativeSymbols
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrTransformer
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
abstract class KlibAssertionWrapperLowering(val context: LoweringContext) : FileLoweringPass {
    private val asserts = (context.symbols as PreSerializationNativeSymbols).asserts.toSet()

    protected abstract val isAssertionArgumentEvaluationEnabled: IrSimpleFunctionSymbol

    fun lower(function: IrFunction) { TODO() }

    override fun lower(irFile: IrFile) { TODO() }

    private inner class Transformer : IrTransformer<IrSymbol>() {
        override fun visitElement(element: IrElement, data: IrSymbol): IrElement { TODO() }

        override fun visitDeclaration(declaration: IrDeclarationBase, data: IrSymbol): IrStatement { TODO() }

        override fun visitCall(expression: IrCall, data: IrSymbol): IrElement { TODO() }
    }
}

/**
 * This lowering replaces `isAssertionThrowingExceptionEnabled` and `isAssertionArgumentErrorEnabled` intrinsics with actual value,
 * depending on the assertion mode.
 */
abstract class KlibAssertionRemoverLowering(
    val context: CommonBackendContext, val throwingErrorEnabled: Boolean, val argumentEvaluationEnabled: Boolean
) : FileLoweringPass {
    protected abstract val isAssertionThrowingErrorEnabled: IrSimpleFunctionSymbol
    protected abstract val isAssertionArgumentEvaluationEnabled: IrSimpleFunctionSymbol

    override fun lower(irFile: IrFile) { TODO() }
}
