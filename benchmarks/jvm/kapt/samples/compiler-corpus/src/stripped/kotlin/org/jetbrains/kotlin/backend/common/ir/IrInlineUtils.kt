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
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeOrFail
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.util.OperatorNameConventions

sealed class IrInlinable
class IrInvokable(val invokable: IrValueDeclaration) : IrInlinable()
class IrInlinableLambda(val function: IrSimpleFunction, val boundArguments: List<IrValueDeclaration>) : IrInlinable()

// Return the underlying function for a lambda argument without bound or default parameters or varargs.
fun IrExpression.asInlinableFunctionReference(): IrFunctionReference? { TODO() }

private fun IrExpression.asInlinableLambda(builder: IrStatementsBuilder<*>): IrInlinableLambda? { TODO() }

fun IrExpression.asInlinable(builder: IrStatementsBuilder<*>): IrInlinable = TODO()

private fun createParameterMapping(source: IrFunction, target: IrFunction): Map<IrValueParameter, IrValueParameter> { TODO() }

fun IrFunction.moveBodyTo(target: IrFunction): IrBody? = TODO()

fun IrFunction.moveBodyTo(target: IrFunction, arguments: Map<IrValueParameter, IrValueDeclaration>): IrBody? = TODO()

private fun IrBody.move(
    source: IrFunction,
    target: IrDeclarationParent,
    targetSymbol: IrReturnTargetSymbol,
    arguments: Map<IrValueParameter, IrValueDeclaration>
): IrBody = TODO()

// TODO use a generic inliner (e.g. JS/Native's FunctionInlining.Inliner)
/** Inline simple function calls without type parameters, default parameters, or varargs.
 * If [moveBody] is true, moves the body into the inlined callsite, otherwise copies it there.
 */
fun IrFunction.inline(
    target: IrDeclarationParent,
    arguments: List<IrValueDeclaration> = emptyList(),
    moveBody: Boolean = true,
): IrReturnableBlock { TODO() }

fun IrInlinable.inline(target: IrDeclarationParent, arguments: List<IrValueDeclaration> = listOf()): IrExpression = TODO()

fun IrInlinedFunctionBlock.getTmpVariablesForArguments(): List<IrVariable> { TODO() }

fun IrInlinedFunctionBlock.getOriginalStatementsFromInlinedBlock(): List<IrStatement> { TODO() }

val IrVariable.isTmpForInline: Boolean
    get() = this.origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE_FOR_INLINED_PARAMETER ||
            this.origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE_FOR_INLINED_EXTENSION_RECEIVER

fun IrExpression.isInlineLambdaBlock(): Boolean { TODO() }

fun IrFunction.isReifiable(): Boolean = TODO()