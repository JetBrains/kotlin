/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalContracts::class)

package org.jetbrains.kotlin.ir.builders.v2

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

// region Return
context(context: IrBuiltInsAware)
fun IrBuilderNew.irReturn(target: IrReturnTargetSymbol, value: IrExpression) =
    IrReturnImpl(
        startOffset, endOffset,
        context.irBuiltIns.nothingType,
        target,
        value
    )
context(context: IrBuiltInsAware, target: IrReturnTargetSymbol)
fun IrBuilderNew.irReturn(value: IrExpression) =
    irReturn(target, value)


context(context: IrBuiltInsAware)
fun IrBuilderNew.irReturnTrue(target: IrReturnTargetSymbol) = irReturn(target, irTrue())
context(context: IrBuiltInsAware)
fun IrBuilderNew.irReturnFalse(target: IrReturnTargetSymbol) = irReturn(target, irFalse())
context(context: IrBuiltInsAware)
fun IrBuilderNew.irReturnUnit(target: IrReturnTargetSymbol) = irReturn(target, irUnit())

context(context: IrBuiltInsAware, target: IrReturnTargetSymbol)
fun IrBuilderNew.irReturnTrue() = irReturn(target, irTrue())
context(context: IrBuiltInsAware, target: IrReturnTargetSymbol)
fun IrBuilderNew.irReturnFalse() = irReturn(target, irFalse())
context(context: IrBuiltInsAware, target: IrReturnTargetSymbol)
fun IrBuilderNew.irReturnUnit() = irReturn(target, irUnit())

// endregion

// region Throw
context(context: IrBuiltInsAware)
fun IrBuilderNew.irThrow(exception: IrExpression): IrThrow =
    IrThrowImpl(
        startOffset, endOffset,
        context.irBuiltIns.nothingType,
        exception
    )
// endregion

// region BreakContinue
context(context: IrBuiltInsAware)
fun IrBuilderNew.irBreak(loop: IrLoop) =
    IrBreakImpl(startOffset, endOffset, context.irBuiltIns.nothingType, loop)

context(context: IrBuiltInsAware)
fun IrBuilderNew.irContinue(loop: IrLoop) =
    IrContinueImpl(startOffset, endOffset, context.irBuiltIns.nothingType, loop)

context(context: IrBuiltInsAware, loop: IrLoop)
fun IrBuilderNew.irBreak() = irBreak(loop)
context(context: IrBuiltInsAware, loop: IrLoop)
fun IrBuilderNew.irContinue() = irContinue(loop)
// endregion

// region When
fun IrBuilderNew.irWhen(type: IrType, branches: List<IrBranch>, origin: IrStatementOrigin? = null): IrWhen =
    IrWhenImpl(startOffset, endOffset, type, origin, branches)

fun IrBuilderNew.irBranch(condition: IrExpression, result: IrExpression): IrBranch =
    IrBranchImpl(startOffset, endOffset, condition, result)

context(context: IrBuiltInsAware)
fun IrBuilderNew.irElseBranch(expression: IrExpression) : IrBranch =
    IrElseBranchImpl(startOffset, endOffset, irTrue(), expression)

class BranchesList {
    val branches = mutableListOf<IrBranch>()
}

context(list: BranchesList)
operator fun Iterable<IrBranch>.unaryPlus() { list.branches.addAll(this) }
context(list: BranchesList)
operator fun IrBranch.unaryPlus() { list.branches.add(this) }

inline fun IrBuilderNew.irWhen(type: IrType, origin: IrStatementOrigin? = null, body: context(BranchesList) IrBuilderNew.() -> Unit) : IrWhen {
    contract { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
    val branches = BranchesList()
    body(branches, this)
    return irWhen(type, branches.branches, origin)
}

context(context: IrBuiltInsAware)
fun IrBuilderNew.irIfThen(condition: IrExpression, thenPart: IrExpression, origin: IrStatementOrigin? = null) =
    irWhen(context.irBuiltIns.unitType, origin) {
        +irBranch(condition, thenPart)
    }

context(context: IrBuiltInsAware)
fun IrBuilderNew.irIfThenElse(
    type: IrType,
    condition: IrExpression,
    thenPart: IrExpression,
    elsePart: IrExpression,
    origin: IrStatementOrigin? = null
) =
    irWhen(type, origin) {
        +irBranch(condition, thenPart)
        +irElseBranch(elsePart)
    }


context(context: IrBuiltInsAware)
fun IrBuilderNew.irIfNull(type: IrType, subject: IrExpression, thenPart: IrExpression, elsePart: IrExpression) =
    irIfThenElse(type, irEqualsNull(subject), thenPart, elsePart)

context(context: IrBuiltInsAware, target: IrReturnTargetSymbol)
fun IrBuilderNew.irIfThenReturnTrue(condition: IrExpression) =
    irIfThen(condition, irReturnTrue())

context(context: IrBuiltInsAware, target: IrReturnTargetSymbol)
fun IrBuilderNew.irIfThenReturnFalse(condition: IrExpression) =
    irIfThen(condition, irReturnFalse())


// endregion

// region Try

fun IrBuilderNew.irTry(type: IrType, tryResult: IrExpression, catches: List<IrCatch>, finallyExpression: IrExpression?): IrTry =
    IrTryImpl(startOffset, endOffset, type, tryResult, catches, finallyExpression)

fun IrBuilderNew.irCatch(catchParameter: IrVariable, result: IrExpression, origin: IrStatementOrigin? = null): IrCatch =
    IrCatchImpl(startOffset, endOffset, catchParameter, result, origin)

class TryScope {
    val catches: MutableList<IrCatch> = mutableListOf()
    var finallyExpression: IrExpression? = null
        internal set
}

context(scope: TryScope, parent: DeclarationParentScope)
fun IrBuilderNew.addCatch(name: Name, type: IrType, result: (IrVariableSymbol) -> IrExpression) {
    val catchParameter = irVariable(name, type, IrDeclarationOrigin.CATCH_PARAMETER)
    scope.catches.add(irCatch(catchParameter, result(catchParameter.symbol)))
}

context(scope: TryScope)
fun IrBuilderNew.setFinally(expression: IrBuilderNew.() -> IrExpression) {
    scope.finallyExpression = expression()
}

context(parent: DeclarationParentScope)
inline fun IrBuilderNew.irTry(type: IrType, result: IrExpression, body: context(TryScope) IrBuilderNew.() -> Unit): IrTry {
    contract { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
    val scope = TryScope()
    body(scope, this)
    return irTry(type, result, scope.catches, scope.finallyExpression)
}


// endregion

// region Loop

context(context: IrBuiltInsAware)
fun IrBuilderNew.irWhile(condition: IrExpression, body: IrExpression?, origin: IrStatementOrigin? = null): IrWhileLoop =
    IrWhileLoopImpl(startOffset, endOffset, context.irBuiltIns.unitType, origin).apply {
        this.condition = condition
        this.body = body
    }

context(context: IrBuiltInsAware)
fun IrBuilderNew.irDoWhile(condition: IrExpression, body: IrExpression?, origin: IrStatementOrigin? = null): IrDoWhileLoop =
    IrDoWhileLoopImpl(startOffset, endOffset, context.irBuiltIns.unitType, origin).apply {
        this.condition = condition
        this.body = body
    }

context(context: IrBuiltInsAware)
inline fun IrBuilderNew.irWhile(condition: IrExpression, origin: IrStatementOrigin? = null, body: context(IrLoop) IrBuilderNew.() -> IrExpression): IrWhileLoop {
    contract { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
    return irWhile(condition, null, origin).apply {
        this.body = body(this, this@irWhile)
    }
}

context(context: IrBuiltInsAware)
inline fun IrBuilderNew.irDoWhile(condition: IrExpression, origin: IrStatementOrigin? = null, body: context(IrLoop) IrBuilderNew.() -> IrExpression): IrDoWhileLoop {
    contract { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
    return irDoWhile(condition, null, origin).apply {
        this.body = body(this, this@irDoWhile)
    }
}

// endregion