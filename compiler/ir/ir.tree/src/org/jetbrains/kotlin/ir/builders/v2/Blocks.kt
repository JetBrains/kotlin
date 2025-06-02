/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalContracts::class)

package org.jetbrains.kotlin.ir.builders.v2

import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrComposite
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInlinedFunctionBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnableBlockImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun IrBuilderNew.irBlock(
    returnType: IrType,
    origin: IrStatementOrigin? = null,
    body: context(StatementList) IrBuilderNew.() -> Unit
): IrBlock {
    contract { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
    val list = StatementList()
    body(list, this)
    return IrBlockImpl(startOffset, endOffset, returnType, origin).apply {
        statements.addAll(list.statements)
    }
}

context(context: IrBuiltInsAware)
inline fun IrBuilderNew.irBlock(
    origin: IrStatementOrigin? = null,
    body: context(StatementList) IrBuilderNew.() -> Unit
): IrBlock {
    contract { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
    val list = StatementList()
    body(list, this)
    return IrBlockImpl(startOffset, endOffset, list.resultType(context.irBuiltIns), origin).apply {
        statements.addAll(list.statements)
    }
}

inline fun IrBuilderNew.irComposite(
    returnType: IrType,
    origin: IrStatementOrigin? = null,
    body: context(StatementList) IrBuilderNew.() -> Unit
): IrComposite {
    contract { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
    val list = StatementList()
    body(list, this)
    return IrCompositeImpl(startOffset, endOffset, returnType, origin).apply {
        statements.addAll(list.statements)
    }
}

context(context: IrBuiltInsAware)
inline fun IrBuilderNew.irComposite(
    origin: IrStatementOrigin? = null,
    body: context(StatementList) IrBuilderNew.() -> Unit
): IrComposite {
    contract { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
    val list = StatementList()
    body(list, this)
    return IrCompositeImpl(startOffset, endOffset, list.resultType(context.irBuiltIns), origin).apply {
        statements.addAll(list.statements)
    }
}

inline fun IrBuilderNew.irBlockBody(
    parent: IrFunction,
    body: context(StatementList, DeclarationParentScope, IrReturnTargetSymbol) IrBuilderNew.() -> Unit
): IrBlockBody {
    contract { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
    val list = StatementList()
    body(list, DeclarationParentScope(parent), parent.symbol, this)
    return parent.factory.createBlockBody(startOffset, endOffset).apply {
        statements.addAll(list.statements)
    }
}

inline fun IrFunction.buildBody(
    body: context(StatementList, DeclarationParentScope, IrReturnTargetSymbol) IrBuilderNew.() -> Unit
) {
    this.body = buildIrAt(this) { irBlockBody(this@buildBody, body) }
}

inline fun IrBuilderNew.irReturnableBlock(
    resultType: IrType,
    symbol: IrReturnableBlockSymbol = IrReturnableBlockSymbolImpl(),
    origin: IrStatementOrigin? = null,
    body: context(StatementList, IrReturnTargetSymbol) IrBuilderNew.() -> Unit
): IrReturnableBlock {
    contract { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
    val block = IrReturnableBlockImpl(startOffset, endOffset, resultType, symbol, origin)
    val list = StatementList()
    body(list, block.symbol, this)
    return block.apply {
        statements.addAll(list.statements)
    }
}

inline fun IrBuilderNew.irInlinedFunctionBlock(
    resultType: IrType,
    origin: IrStatementOrigin? = null,
    inlinedFunctionStartOffset: Int,
    inlinedFunctionEndOffset: Int,
    inlinedFunctionSymbol: IrFunctionSymbol?,
    inlinedFunctionFileEntry: IrFileEntry,
    body: context(StatementList) IrBuilderNew.() -> Unit
): IrInlinedFunctionBlock {
    contract { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
    val list = StatementList()
    body(list, this)
    return IrInlinedFunctionBlockImpl(
        startOffset = startOffset,
        endOffset = endOffset,
        inlinedFunctionStartOffset = inlinedFunctionStartOffset,
        inlinedFunctionEndOffset = inlinedFunctionEndOffset,
        type = resultType,
        inlinedFunctionSymbol = inlinedFunctionSymbol,
        inlinedFunctionFileEntry = inlinedFunctionFileEntry,
        origin = origin,
    ).apply {
        statements.addAll(list.statements)
    }
}

context(context: IrBuiltInsAware)
inline fun IrBuilderNew.irInlinedFunctionBlock(
    origin: IrStatementOrigin? = null,
    inlinedFunctionStartOffset: Int,
    inlinedFunctionEndOffset: Int,
    inlinedFunctionSymbol: IrFunctionSymbol?,
    inlinedFunctionFileEntry: IrFileEntry,
    body: context(StatementList) IrBuilderNew.() -> Unit
): IrInlinedFunctionBlock {
    contract { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
    val list = StatementList()
    body(list, this)
    return IrInlinedFunctionBlockImpl(
        startOffset = startOffset,
        endOffset = endOffset,
        inlinedFunctionStartOffset = inlinedFunctionStartOffset,
        inlinedFunctionEndOffset = inlinedFunctionEndOffset,
        type = list.resultType(context.irBuiltIns),
        inlinedFunctionSymbol = inlinedFunctionSymbol,
        inlinedFunctionFileEntry = inlinedFunctionFileEntry,
        origin = origin,
    ).apply {
        statements.addAll(list.statements)
    }
}

fun IrBuilderNew.irExprBody(value: IrExpression) =
    IrFactoryImpl.createExpressionBody(startOffset, endOffset, value)
