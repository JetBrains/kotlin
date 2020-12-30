/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.backend.common.lower.VariableRemapper
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnableBlockImpl
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
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

private fun createParameterMapping(source: IrFunction, target: IrFunction): Map<IrValueParameter, IrValueParameter> {
    val sourceParameters = listOfNotNull(source.dispatchReceiverParameter, source.extensionReceiverParameter) + source.valueParameters
    val targetParameters = listOfNotNull(target.dispatchReceiverParameter, target.extensionReceiverParameter) + target.valueParameters
    assert(sourceParameters.size == targetParameters.size)
    return sourceParameters.zip(targetParameters).toMap()
}

fun IrFunction.moveBodyTo(target: IrFunction): IrBody? =
    moveBodyTo(target, createParameterMapping(this, target))

fun IrFunction.moveBodyTo(target: IrFunction, arguments: Map<IrValueParameter, IrValueDeclaration>): IrBody? =
    body?.move(this, target, target.symbol, arguments)

private fun IrBody.move(
    source: IrFunction,
    target: IrDeclarationParent,
    targetSymbol: IrReturnTargetSymbol,
    arguments: Map<IrValueParameter, IrValueDeclaration>
): IrBody = transform(object : VariableRemapper(arguments) {
    override fun visitReturn(expression: IrReturn): IrExpression = super.visitReturn(
        if (expression.returnTargetSymbol == source.symbol)
            IrReturnImpl(expression.startOffset, expression.endOffset, expression.type, targetSymbol, expression.value)
        else
            expression
    )

    override fun visitBlock(expression: IrBlock): IrExpression {
        // Might be an inline lambda argument; if the function has already been moved out, visit it explicitly.
        if (expression.origin == IrStatementOrigin.LAMBDA || expression.origin == IrStatementOrigin.ANONYMOUS_FUNCTION)
            if (expression.statements.lastOrNull() is IrFunctionReference && expression.statements.none { it is IrFunction })
                (expression.statements.last() as IrFunctionReference).symbol.owner.transformChildrenVoid()
        return super.visitBlock(expression)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
        if (declaration.parent == source)
            declaration.parent = target
        return super.visitDeclaration(declaration)
    }
}, null)

// TODO use a generic inliner (e.g. JS/Native's FunctionInlining.Inliner)
// Inline simple function calls without type parameters, default parameters, or varargs.
fun IrFunction.inline(target: IrDeclarationParent, arguments: List<IrValueDeclaration> = listOf()): IrReturnableBlock =
    IrReturnableBlockImpl(startOffset, endOffset, returnType, IrReturnableBlockSymbolImpl(), null, symbol).apply {
        statements += body!!.move(this@inline, target, symbol, valueParameters.zip(arguments).toMap()).statements
    }
