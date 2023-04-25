/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.backend.common.lower.VariableRemapper
import org.jetbrains.kotlin.backend.common.lower.inline.INLINED_FUNCTION_ARGUMENTS
import org.jetbrains.kotlin.backend.common.lower.inline.INLINED_FUNCTION_DEFAULT_ARGUMENTS
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.IrStatementsBuilder
import org.jetbrains.kotlin.ir.builders.irComposite
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.util.OperatorNameConventions

sealed class IrInlinable
class IrInvokable(val invokable: IrValueDeclaration) : IrInlinable()
class IrInlinableLambda(val function: IrSimpleFunction, val boundReceiver: IrValueDeclaration?) : IrInlinable()

// Return the underlying function for a lambda argument without bound or default parameters or varargs.
fun IrExpression.asInlinableFunctionReference(): IrFunctionReference? {
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
    return reference
}

private fun IrExpression.asInlinableLambda(builder: IrStatementsBuilder<*>): IrInlinableLambda? {
    if (this is IrFunctionExpression) {
        if (function.valueParameters.any { it.isVararg || it.defaultValue != null })
            return null
        return IrInlinableLambda(function, null)
    }
    return asInlinableFunctionReference()?.let { reference ->
        IrInlinableLambda(reference.symbol.owner as IrSimpleFunction, reference.extensionReceiver?.let { builder.irTemporary(it) })
    }
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
fun IrFunction.inline(target: IrDeclarationParent, arguments: List<IrValueDeclaration> = listOf()): IrReturnableBlock =
    IrReturnableBlockImpl(startOffset, endOffset, returnType, IrReturnableBlockSymbolImpl(), null).apply {
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

val IrInlinedFunctionBlock.inlineDeclaration: IrDeclaration
    get() = when (val element = inlinedElement) {
        is IrFunction -> element
        is IrFunctionExpression -> element.function
        is IrFunctionReference -> element.symbol.owner
        is IrPropertyReference -> element.symbol.owner
        else -> throw AssertionError("Not supported ir element for inlining ${element.dump()}")
    }

private val IrInlinedFunctionBlock.inlineFunction: IrFunction?
    get() = when (val element = inlinedElement) {
        is IrFunction -> element
        is IrFunctionExpression -> element.function
        is IrFunctionReference -> element.symbol.owner.takeIf { it.isInline }
        else -> null
    }

fun IrInlinedFunctionBlock.isFunctionInlining(): Boolean {
    return this.inlinedElement is IrFunction
}

fun IrInlinedFunctionBlock.isLambdaInlining(): Boolean {
    return !isFunctionInlining()
}

fun IrInlinedFunctionBlock.getAdditionalStatementsFromInlinedBlock(): List<IrStatement> {
    return this.statements
        .filterIsInstance<IrComposite>()
        .filter { it.origin == INLINED_FUNCTION_ARGUMENTS || it.origin == INLINED_FUNCTION_DEFAULT_ARGUMENTS }
        .flatMap { it.statements }
}

fun IrInlinedFunctionBlock.getNonDefaultAdditionalStatementsFromInlinedBlock(): List<IrStatement> {
    return this.statements
        .filterIsInstance<IrComposite>()
        .singleOrNull { it.origin == INLINED_FUNCTION_ARGUMENTS }?.statements ?: emptyList()
}

fun IrInlinedFunctionBlock.getDefaultAdditionalStatementsFromInlinedBlock(): List<IrStatement> {
    return this.statements
        .filterIsInstance<IrComposite>()
        .singleOrNull { it.origin == INLINED_FUNCTION_DEFAULT_ARGUMENTS }?.statements ?: emptyList()
}

fun IrInlinedFunctionBlock.getOriginalStatementsFromInlinedBlock(): List<IrStatement> {
    return this.statements
        .filter { it !is IrComposite || !(it.origin == INLINED_FUNCTION_ARGUMENTS || it.origin == INLINED_FUNCTION_DEFAULT_ARGUMENTS) }
}

fun IrInlinedFunctionBlock.putStatementBeforeActualInline(builder: IrBuilderWithScope, statement: IrStatement) {
    val evaluateStatements = this.statements
        .filterIsInstance<IrComposite>()
        .singleOrNull { it.origin == INLINED_FUNCTION_ARGUMENTS }?.statements

    if (evaluateStatements != null) {
        evaluateStatements.add(0, statement)
        return
    }

    val newInlinedArgumentsBlock = builder
        .irComposite(UNDEFINED_OFFSET, UNDEFINED_OFFSET, INLINED_FUNCTION_ARGUMENTS, builder.context.irBuiltIns.unitType) { +statement }
    this.statements.add(0, newInlinedArgumentsBlock)
}

fun IrInlinedFunctionBlock.putStatementsInFrontOfInlinedFunction(statements: List<IrStatement>) {
    val insertAfter = this.statements
        .indexOfLast { it is IrComposite && (it.origin == INLINED_FUNCTION_ARGUMENTS || it.origin == INLINED_FUNCTION_DEFAULT_ARGUMENTS) }

    this.statements.addAll(if (insertAfter == -1) 0 else insertAfter + 1, statements)
}

val IrContainerExpression.innerInlinedBlockOrThis: IrContainerExpression
    get() = (this as? IrReturnableBlock)?.statements?.singleOrNull() as? IrInlinedFunctionBlock ?: this

val IrReturnableBlock.inlineFunction: IrFunction?
    get() = (this.statements.singleOrNull() as? IrInlinedFunctionBlock)?.inlineFunction

val IrReturnableBlock.sourceFileSymbol: IrFileSymbol?
    get() = inlineFunction?.fileOrNull?.symbol
