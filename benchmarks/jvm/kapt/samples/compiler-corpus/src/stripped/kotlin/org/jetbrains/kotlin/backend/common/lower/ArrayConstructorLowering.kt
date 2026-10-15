/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ir.asInlinable
import org.jetbrains.kotlin.backend.common.ir.inline
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * Transforms `Array(size) { index -> value }` into a loop.
 */
class ArrayConstructorLowering(private val context: LoweringContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) { TODO() }
}

private class ArrayConstructorTransformer(
    val context: LoweringContext,
    val container: IrSymbolOwner
) : IrElementTransformerVoidWithContext() {

    // Array(size, init) -> Array(size)
    companion object {
        internal fun arrayInlineToSizeConstructor(context: LoweringContext, irConstructor: IrConstructor): IrFunctionSymbol? { TODO() }
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression { TODO() }
}
