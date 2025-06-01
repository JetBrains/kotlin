/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.v2

import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

fun IrBuilderNew.irCall(
    callee: IrSimpleFunctionSymbol,
    type: IrType,
    origin: IrStatementOrigin? = null,
): IrCall =
    IrCallImpl(
        startOffset, endOffset,
        type,
        callee,
        origin = origin
    )

fun IrBuilderNew.irCall(
    callee: IrConstructorSymbol,
    type: IrType,
    origin: IrStatementOrigin? = null
): IrConstructorCall =
    IrConstructorCallImpl(
        startOffset, endOffset, type, callee,
        typeArgumentsCount = callee.owner.typeParameters.size + callee.owner.parentAsClass.typeParameters.size,
        constructorTypeArgumentsCount = callee.owner.typeParameters.size,
        origin = origin,
    )

fun IrBuilderNew.irCall(callee: IrFunctionSymbol, type: IrType): IrFunctionAccessExpression =
    when (callee) {
        is IrConstructorSymbol -> irCall(callee, type)
        is IrSimpleFunctionSymbol -> irCall(callee, type)
    }

fun IrBuilderNew.irCall(
    callee: IrSimpleFunctionSymbol,
    type: IrType,
    typeArguments: List<IrType>,
    arguments: List<IrExpression?>,
    origin: IrStatementOrigin? = null,
): IrCall =
    irCall(callee, type, origin).apply {
        require(this.typeArguments.size == typeArguments.size)
        require(this.arguments.size == arguments.size)
        this.typeArguments.assignFrom(typeArguments)
        this.arguments.assignFrom(arguments)
    }

fun IrBuilderNew.irCall(
    callee: IrConstructorSymbol,
    type: IrType,
    typeArguments: List<IrType>,
    arguments: List<IrExpression?>,
    origin: IrStatementOrigin? = null
): IrConstructorCall =
    irCall(callee, type, origin).apply {
        require(this.typeArguments.size == typeArguments.size)
        require(this.arguments.size == arguments.size)
        this.typeArguments.assignFrom(typeArguments)
        this.arguments.assignFrom(arguments)
    }

fun IrBuilderNew.irCall(
    callee: IrFunctionSymbol,
    type: IrType,
    typeArguments: List<IrType>,
    arguments: List<IrExpression?>,
    origin: IrStatementOrigin? = null
): IrFunctionAccessExpression =
    when (callee) {
        is IrConstructorSymbol -> irCall(callee, type, typeArguments, arguments, origin)
        is IrSimpleFunctionSymbol -> irCall(callee, type, typeArguments, arguments, origin)
    }

fun IrBuilderNew.irSuperQualifiedCall(
    callee: IrSimpleFunctionSymbol,
    superQualifierSymbol: IrClassSymbol?,
    type: IrType,
    typeArguments: List<IrType>,
    arguments: List<IrExpression?>,
    origin: IrStatementOrigin? = null,
): IrCall =
    irCall(callee, type, origin).apply {
        this.superQualifierSymbol = superQualifierSymbol
        require(this.typeArguments.size == typeArguments.size)
        require(this.arguments.size == arguments.size)
        this.typeArguments.assignFrom(typeArguments)
        this.arguments.assignFrom(arguments)
    }