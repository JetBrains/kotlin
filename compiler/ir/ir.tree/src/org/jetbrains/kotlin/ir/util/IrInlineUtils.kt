/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol

val IrInlinedFunctionBlock.inlineDeclaration: IrDeclaration
    get() = inlinedFunctionSymbol?.owner ?: (inlinedExpression as? IrPropertyReference)?.symbol?.owner
    ?: throw AssertionError("Not supported ir element for inlining ${inlinedExpression?.dump()}")

val IrInlinedFunctionBlock.inlineFunction: IrFunction?
    get() = inlinedFunctionSymbol?.owner?.takeIf { it.isInline || inlinedExpression !is IrFunctionReference }

fun IrInlinedFunctionBlock.isFunctionInlining() = inlinedExpression == null

fun IrInlinedFunctionBlock.isLambdaInlining(): Boolean {
    return !isFunctionInlining()
}

val IrContainerExpression.innerInlinedBlockOrThis: IrContainerExpression
    get() = (this as? IrReturnableBlock)?.statements?.singleOrNull() as? IrInlinedFunctionBlock ?: this
val IrReturnableBlock.inlineFunction: IrFunction?
    get() = (this.statements.singleOrNull() as? IrInlinedFunctionBlock)?.inlineFunction
val IrReturnableBlock.sourceFileSymbol: IrFileSymbol?
    get() = inlineFunction?.fileOrNull?.symbol