/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.backend.common.lower.VariableRemapper
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnableBlockImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.statements

// Return the underlying function for a lambda argument without bound or default parameters or varargs.
fun IrExpression.asSimpleLambda(): IrSimpleFunction? {
    if (this is IrFunctionExpression) {
        if (function.valueParameters.any { it.isVararg || it.defaultValue != null })
            return null
        return function
    }
    // A lambda is represented as a block with a function declaration and a reference to it.
    if (this !is IrBlock || statements.size != 2)
        return null
    val (function, reference) = statements
    if (function !is IrSimpleFunction || reference !is IrFunctionReference || function.symbol != reference.symbol)
        return null
    if ((0 until reference.valueArgumentsCount).any { reference.getValueArgument(it) != null })
        return null
    if (function.valueParameters.any { it.isVararg || it.defaultValue != null })
        return null
    return function
}

// TODO use a generic inliner (e.g. JS/Native's FunctionInlining.Inliner)
// Inline simple function calls without type parameters, default parameters, or varargs.
fun IrFunction.inline(arguments: List<IrValueDeclaration> = listOf()): IrReturnableBlock {
    require(body != null)
    val argumentMap = valueParameters.zip(arguments).toMap()
    val blockSymbol = IrReturnableBlockSymbolImpl(descriptor)
    val block = IrReturnableBlockImpl(startOffset, endOffset, returnType, blockSymbol, null, symbol)
    val remapper = object : VariableRemapper(argumentMap) {
        override fun visitReturn(expression: IrReturn): IrExpression = super.visitReturn(
            if (expression.returnTargetSymbol == symbol)
                IrReturnImpl(expression.startOffset, expression.endOffset, expression.type, blockSymbol, expression.value)
            else
                expression
        )
    }
    body!!.statements.mapTo(block.statements) { it.transform(remapper, null) }
    return block
}
