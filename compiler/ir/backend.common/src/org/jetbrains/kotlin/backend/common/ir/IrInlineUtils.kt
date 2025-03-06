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
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.util.OperatorNameConventions

sealed class IrInlinable
class IrInvokable(val invokable: IrValueDeclaration) : IrInlinable()
class IrInlinableLambda(val function: IrSimpleFunction, val boundArguments: List<IrValueDeclaration>) : IrInlinable()

// Return the underlying function for a lambda argument without bound or default parameters or varargs.
fun IrExpression.asInlinableFunctionReference(): IrFunctionReference? {
    // A lambda is represented as a block with a function declaration and a reference to it.
    // Inlinable function references are also a kind of lambda; bound receivers are represented as extension receivers.
    if (this !is IrBlock || statements.size != 2)
        return null
    val (function, reference) = statements
    if (function !is IrSimpleFunction || reference !is IrFunctionReference || function.symbol != reference.symbol)
        return null
    if (reference.arguments.zip(reference.symbol.owner.parameters)
            .any { (argument, parameter) -> parameter.kind != IrParameterKind.ExtensionReceiver && argument != null }
    ) return null
    if (function.parameters.any { it.isVararg || it.defaultValue != null })
        return null
    return reference
}

private fun IrExpression.asInlinableLambda(builder: IrStatementsBuilder<*>): IrInlinableLambda? {
    when (this) {
        is IrRichCallableReference<*> -> {
            return IrInlinableLambda(invokeFunction, boundValues.map { builder.irTemporary(it) })
        }
        is IrFunctionExpression -> {
            if (function.parameters.any { it.isVararg || it.defaultValue != null })
                return null
            return IrInlinableLambda(function, emptyList())
        }
        else -> return asInlinableFunctionReference()?.let { reference ->
            IrInlinableLambda(
                reference.symbol.owner as IrSimpleFunction,
                listOfNotNull(reference.extensionReceiver?.let { builder.irTemporary(it) })
            )
        }
    }
}

fun IrExpression.asInlinable(builder: IrStatementsBuilder<*>): IrInlinable =
    asInlinableLambda(builder) ?: IrInvokable(builder.irTemporary(this))

private fun createParameterMapping(source: IrFunction, target: IrFunction): Map<IrValueParameter, IrValueParameter> {
    val sourceParameters = source.parameters
    val targetParameters = target.parameters
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
    IrReturnableBlockImpl(startOffset, endOffset, returnType, IrReturnableBlockSymbolImpl(), null).apply {
        statements += body!!.move(this@inline, target, symbol, parameters.zip(arguments).toMap()).statements
    }

fun IrInlinable.inline(target: IrDeclarationParent, arguments: List<IrValueDeclaration> = listOf()): IrExpression =
    when (this) {
        is IrInlinableLambda ->
            function.inline(target, boundArguments + arguments)

        is IrInvokable -> {
            val invoke = invokable.type.getClass()!!.functions.single { it.name == OperatorNameConventions.INVOKE }
            IrCallImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, invoke.returnType, invoke.symbol,
                typeArgumentsCount = 0,
            ).apply {
                val newArguments = (listOf(invokable) + arguments).map { arg ->
                    IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, arg.symbol)
                }
                for ((index, argument) in newArguments.withIndex()) {
                    this.arguments[index] = argument
                }
            }
        }
    }

fun IrInlinedFunctionBlock.getTmpVariablesForArguments(): List<IrVariable> {
    return this.statements.filterIsInstance<IrVariable>().filter { it.isTmpForInline }
}

fun IrInlinedFunctionBlock.getOriginalStatementsFromInlinedBlock(): List<IrStatement> {
    return this.statements.filterNot { it is IrVariable && it.isTmpForInline }
}

val IrVariable.isTmpForInline: Boolean
    get() = this.origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE_FOR_INLINED_PARAMETER ||
            this.origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE_FOR_INLINED_EXTENSION_RECEIVER

fun IrExpression.isInlineLambdaBlock(): Boolean {
    if (!this.isLambdaBlock()) return false

    val block = this as IrBlock
    val reference = block.statements.last() as? IrFunctionReference
    return reference?.origin == IrStatementOrigin.INLINE_LAMBDA
}

fun IrFunction.isReifiable(): Boolean =
    typeParameters.any { it.isReified }
