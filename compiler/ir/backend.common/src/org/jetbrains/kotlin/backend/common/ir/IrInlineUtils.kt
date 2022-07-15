/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.backend.common.lower.VariableRemapper
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrStatementsBuilder
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnableBlockImpl
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.util.OperatorNameConventions

sealed class IrInlinable
class IrInvokable(val invokable: IrValueDeclaration) : IrInlinable()
class IrInlinableLambda(val function: IrSimpleFunction, val boundReceiver: IrValueDeclaration?) : IrInlinable()

// Return the underlying function for a lambda argument without bound or default parameters or varargs.
private fun IrExpression.asInlinableLambda(builder: IrStatementsBuilder<*>): IrInlinableLambda? {
    if (this is IrFunctionExpression) {
        if (function.valueParameters.any { it.isVararg || it.defaultValue != null })
            return null
        return IrInlinableLambda(function, null)
    }
    // A lambda is represented as a block with a function declaration and a reference to it.
    // Inlinable function references are also a kind of lambda; bound receivers are represented as extension receivers.
    if (this !is IrBlock || statements.size != 2)
        return null
    val (function, reference) = statements
    if (function !is IrSimpleFunction || reference !is IrFunctionReference || function.symbol != reference.symbol)
        return null
    if (function.dispatchReceiverParameter != null)
        return null
    if ((0 until reference.valueArgumentsCount).any { reference.getValueArgument(it) != null })
        return null
    if (function.valueParameters.any { it.isVararg || it.defaultValue != null })
        return null
    return IrInlinableLambda(function, reference.extensionReceiver?.let { builder.irTemporary(it) })
}

fun IrExpression.asInlinable(builder: IrStatementsBuilder<*>): IrInlinable =
    asInlinableLambda(builder) ?: IrInvokable(builder.irTemporary(this))

private fun createParameterMapping(source: IrFunction, target: IrFunction): Map<IrValueParameter, IrValueParameter> {
    val sourceParameters = source.explicitParameters
    val targetParameters = target.explicitParameters
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
private fun IrFunction.inline(target: IrDeclarationParent, arguments: List<IrValueDeclaration> = listOf()): IrReturnableBlock =
    IrReturnableBlockImpl(startOffset, endOffset, returnType, IrReturnableBlockSymbolImpl(), null, symbol).apply {
        statements += body!!.move(this@inline, target, symbol, explicitParameters.zip(arguments).toMap()).statements
    }

fun IrInlinable.inline(target: IrDeclarationParent, arguments: List<IrValueDeclaration> = listOf()): IrExpression =
    when (this) {
        is IrInlinableLambda ->
            function.inline(target, listOfNotNull(boundReceiver) + arguments)

        is IrInvokable -> {
            val invoke = invokable.type.getClass()!!.functions.single { it.name == OperatorNameConventions.INVOKE }
            IrCallImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, invoke.returnType, invoke.symbol,
                typeArgumentsCount = 0, valueArgumentsCount = arguments.size,
            ).apply {
                dispatchReceiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, invokable.symbol)
                for ((index, argument) in arguments.withIndex()) {
                    putValueArgument(index, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, argument.symbol))
                }
            }
        }
    }
