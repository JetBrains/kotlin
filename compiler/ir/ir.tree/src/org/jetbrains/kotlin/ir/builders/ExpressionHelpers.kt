/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.isImmutable
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.assertedCast

val IrBuilderWithScope.parent get() = scope.getLocalDeclarationParent()

inline fun IrBuilderWithScope.irLetS(
    value: IrExpression,
    origin: IrStatementOrigin? = null,
    nameHint: String? = null,
    irType: IrType? = null,
    body: (IrValueSymbol) -> IrExpression
): IrExpression {
    val (valueSymbol, irTemporary) = if (value is IrGetValue && value.symbol.owner.isImmutable) {
        value.symbol to null
    } else {
        scope.createTemporaryVariable(value, nameHint, irType = irType).let { it.symbol to it }
    }
    val irResult = body(valueSymbol)
    return if (irTemporary == null) {
        irResult
    } else {
        val irBlock = IrBlockImpl(startOffset, endOffset, irResult.type, origin)
        irBlock.statements.add(irTemporary)
        irBlock.statements.add(irResult)
        irBlock
    }
}


fun <T : IrElement> IrStatementsBuilder<T>.irTemporary(
    value: IrExpression,
    nameHint: String? = null,
    typeHint: KotlinType? = null,
    irType: IrType? = null
): IrVariable {
    val temporary = scope.createTemporaryVariable(value, nameHint, type = typeHint, irType = irType)
    +temporary
    return temporary
}

fun <T : IrElement> IrStatementsBuilder<T>.irTemporaryVarDeclaration(
    type: IrType,
    nameHint: String? = null,
    isMutable: Boolean = true
): IrVariable {
    val temporary = scope.createTemporaryVariableDeclaration(type, nameHint, isMutable = isMutable)
    +temporary
    return temporary
}

fun <T : IrElement> IrStatementsBuilder<T>.irTemporaryVar(
    value: IrExpression,
    nameHint: String? = null,
    typeHint: KotlinType? = null
): IrVariable {
    val temporary = scope.createTemporaryVariable(value, nameHint, isMutable = true, type = typeHint)
    +temporary
    return temporary
}


fun IrBuilderWithScope.irExprBody(value: IrExpression) =
    context.irFactory.createExpressionBody(startOffset, endOffset, value)

fun IrBuilderWithScope.irWhen(type: IrType, branches: List<IrBranch>) =
    IrWhenImpl(startOffset, endOffset, type, null, branches)

fun IrBuilderWithScope.irReturn(value: IrExpression) =
    IrReturnImpl(
        startOffset, endOffset,
        context.irBuiltIns.nothingType,
        scope.scopeOwnerSymbol.assertedCast<IrReturnTargetSymbol> {
            "Function scope expected: ${scope.scopeOwnerSymbol.owner.render()}"
        },
        value
    )

fun IrBuilderWithScope.irBoolean(value: Boolean) =
    IrConstImpl(startOffset, endOffset, context.irBuiltIns.booleanType, IrConstKind.Boolean, value)

fun IrBuilderWithScope.irUnit() =
    irGetObjectValue(context.irBuiltIns.unitType, context.irBuiltIns.unitClass)

fun IrBuilderWithScope.irTrue() = irBoolean(true)
fun IrBuilderWithScope.irFalse() = irBoolean(false)
fun IrBuilderWithScope.irReturnTrue() = irReturn(irTrue())
fun IrBuilderWithScope.irReturnFalse() = irReturn(irFalse())
fun IrBuilderWithScope.irReturnUnit() = irReturn(irUnit())

fun IrBuilderWithScope.irBranch(condition: IrExpression, result: IrExpression) =
    IrBranchImpl(startOffset, endOffset, condition, result)

fun IrBuilderWithScope.irElseBranch(expression: IrExpression) =
    IrElseBranchImpl(startOffset, endOffset, irTrue(), expression)

fun IrBuilderWithScope.irIfThen(type: IrType, condition: IrExpression, thenPart: IrExpression, origin: IrStatementOrigin? = null) =
    IrIfThenElseImpl(startOffset, endOffset, type, origin).apply {
        branches.add(IrBranchImpl(startOffset, endOffset, condition, thenPart))
    }

fun IrBuilderWithScope.irIfThenElse(
    type: IrType,
    condition: IrExpression,
    thenPart: IrExpression,
    elsePart: IrExpression,
    origin: IrStatementOrigin? = null
) =
    IrIfThenElseImpl(startOffset, endOffset, type, origin).apply {
        branches.add(IrBranchImpl(startOffset, endOffset, condition, thenPart))
        branches.add(irElseBranch(elsePart))
    }

fun IrBuilderWithScope.irIfThenMaybeElse(
    type: IrType,
    condition: IrExpression,
    thenPart: IrExpression,
    elsePart: IrExpression?,
    origin: IrStatementOrigin? = null
) =
    if (elsePart != null)
        irIfThenElse(type, condition, thenPart, elsePart, origin)
    else
        irIfThen(type, condition, thenPart, origin)

fun IrBuilderWithScope.irIfNull(type: IrType, subject: IrExpression, thenPart: IrExpression, elsePart: IrExpression) =
    irIfThenElse(type, irEqualsNull(subject), thenPart, elsePart)

fun IrBuilderWithScope.irIfThenReturnTrue(condition: IrExpression) =
    irIfThen(context.irBuiltIns.unitType, condition, irReturnTrue())

fun IrBuilderWithScope.irIfThenReturnFalse(condition: IrExpression) =
    irIfThen(context.irBuiltIns.unitType, condition, irReturnFalse())

fun IrBuilderWithScope.irGet(type: IrType, variable: IrValueSymbol) =
    IrGetValueImpl(startOffset, endOffset, type, variable)

fun IrBuilderWithScope.irGet(variable: IrValueDeclaration) = irGet(variable.type, variable.symbol)

fun IrBuilderWithScope.irSetVar(variable: IrVariableSymbol, value: IrExpression) =
    IrSetVariableImpl(startOffset, endOffset, context.irBuiltIns.unitType, variable, value, IrStatementOrigin.EQ)

fun IrBuilderWithScope.irGetField(receiver: IrExpression?, field: IrField) =
    IrGetFieldImpl(startOffset, endOffset, field.symbol, field.type, receiver)

fun IrBuilderWithScope.irSetField(receiver: IrExpression?, field: IrField, value: IrExpression) =
    IrSetFieldImpl(startOffset, endOffset, field.symbol, receiver, value, context.irBuiltIns.unitType)

fun IrBuilderWithScope.irGetObjectValue(type: IrType, classSymbol: IrClassSymbol) =
    IrGetObjectValueImpl(startOffset, endOffset, type, classSymbol)

fun IrBuilderWithScope.irEqeqeq(arg1: IrExpression, arg2: IrExpression) =
    context.eqeqeq(startOffset, endOffset, arg1, arg2)

fun IrBuilderWithScope.irNull() =
    irNull(context.irBuiltIns.nothingNType)

fun IrBuilderWithScope.irNull(irType: IrType) =
    IrConstImpl.constNull(startOffset, endOffset, irType)

fun IrBuilderWithScope.irEqualsNull(argument: IrExpression) =
    irEquals(argument, irNull())

fun IrBuilderWithScope.irEquals(arg1: IrExpression, arg2: IrExpression, origin: IrStatementOrigin = IrStatementOrigin.EQEQ) =
    primitiveOp2(
        startOffset, endOffset, context.irBuiltIns.eqeqSymbol, context.irBuiltIns.booleanType, origin,
        arg1, arg2
    )

fun IrBuilderWithScope.irNotEquals(arg1: IrExpression, arg2: IrExpression) =
    primitiveOp1(
        startOffset, endOffset, context.irBuiltIns.booleanNotSymbol, context.irBuiltIns.booleanType, IrStatementOrigin.EXCLEQ,
        irEquals(arg1, arg2, origin = IrStatementOrigin.EXCLEQ)
    )

fun IrBuilderWithScope.irGet(type: IrType, receiver: IrExpression?, getterSymbol: IrFunctionSymbol): IrCall =
    IrCallImpl(
        startOffset, endOffset,
        type,
        getterSymbol as IrSimpleFunctionSymbol,
        typeArgumentsCount = getterSymbol.owner.typeParameters.size,
        valueArgumentsCount = 0,
        origin = IrStatementOrigin.GET_PROPERTY
    ).apply {
        dispatchReceiver = receiver
    }

fun IrBuilderWithScope.irSet(type: IrType, receiver: IrExpression?, setterSymbol: IrFunctionSymbol, value: IrExpression): IrCall =
    IrCallImpl(
        startOffset, endOffset,
        type,
        setterSymbol as IrSimpleFunctionSymbol,
        typeArgumentsCount = setterSymbol.owner.typeParameters.size,
        valueArgumentsCount = 1,
        origin = IrStatementOrigin.EQ
    ).apply {
        dispatchReceiver = receiver
        putValueArgument(0, value)
    }

fun IrBuilderWithScope.irCall(
    callee: IrFunctionSymbol,
    type: IrType,
    typeArguments: List<IrType>
): IrMemberAccessExpression<*> =
    irCall(callee, type).apply {
        typeArguments.forEachIndexed { index, irType ->
            this.putTypeArgument(index, irType)
        }
    }

fun IrBuilderWithScope.irCallConstructor(callee: IrConstructorSymbol, typeArguments: List<IrType>): IrConstructorCall =
    IrConstructorCallImpl.fromSymbolOwner(
        startOffset,
        endOffset,
        callee.owner.returnType,
        callee,
        typeArguments.size - callee.owner.typeParameters.size
    ).apply {
        typeArguments.forEachIndexed { index, irType ->
            this.putTypeArgument(index, irType)
        }
    }

fun IrBuilderWithScope.irCall(
    callee: IrSimpleFunctionSymbol,
    type: IrType,
    valueArgumentsCount: Int = callee.owner.valueParameters.size,
    typeArgumentsCount: Int = callee.owner.typeParameters.size
): IrCall =
    IrCallImpl(
        startOffset, endOffset, type, callee,
        typeArgumentsCount = typeArgumentsCount,
        valueArgumentsCount = valueArgumentsCount
    )

fun IrBuilderWithScope.irCall(
    callee: IrConstructorSymbol,
    type: IrType,
    constructedClass: IrClass = callee.owner.parentAsClass
): IrConstructorCall =
    IrConstructorCallImpl(
        startOffset, endOffset, type, callee,
        valueArgumentsCount = callee.owner.valueParameters.size,
        typeArgumentsCount = callee.owner.typeParameters.size + constructedClass.typeParameters.size,
        constructorTypeArgumentsCount = callee.owner.typeParameters.size
    )

fun IrBuilderWithScope.irCall(callee: IrFunctionSymbol, type: IrType): IrFunctionAccessExpression =
    when (callee) {
        is IrConstructorSymbol -> irCall(callee, type)
        is IrSimpleFunctionSymbol -> irCall(callee, type)
        else -> throw AssertionError("Unexpected callee: $callee")
    }

fun IrBuilderWithScope.irCall(callee: IrSimpleFunctionSymbol): IrCall =
    irCall(callee, callee.owner.returnType)

fun IrBuilderWithScope.irCall(callee: IrConstructorSymbol): IrConstructorCall =
    irCall(callee, callee.owner.returnType)

fun IrBuilderWithScope.irCall(callee: IrFunctionSymbol): IrFunctionAccessExpression =
    irCall(callee, callee.owner.returnType)

fun IrBuilderWithScope.irCall(callee: IrFunctionSymbol, descriptor: FunctionDescriptor, type: IrType): IrCall =
    IrCallImpl(
        startOffset, endOffset, type,
        callee as IrSimpleFunctionSymbol,
        valueArgumentsCount = callee.owner.valueParameters.size,
        typeArgumentsCount = callee.owner.typeParameters.size
    )

fun IrBuilderWithScope.irCall(callee: IrFunction): IrFunctionAccessExpression =
    irCall(callee.symbol)

fun IrBuilderWithScope.irCall(callee: IrFunction, origin: IrStatementOrigin? = null, superQualifierSymbol: IrClassSymbol? = null): IrCall =
    IrCallImpl(
        startOffset, endOffset, callee.returnType,
        callee.symbol as IrSimpleFunctionSymbol,
        callee.typeParameters.size, callee.valueParameters.size,
        origin, superQualifierSymbol
    )

fun IrBuilderWithScope.irDelegatingConstructorCall(callee: IrConstructor): IrDelegatingConstructorCall =
    IrDelegatingConstructorCallImpl(
        startOffset, endOffset, context.irBuiltIns.unitType, callee.symbol,
        callee.parentAsClass.typeParameters.size, callee.valueParameters.size
    )

fun IrBuilderWithScope.irCallOp(
    callee: IrSimpleFunctionSymbol,
    type: IrType,
    dispatchReceiver: IrExpression,
    argument: IrExpression? = null
): IrMemberAccessExpression<*> =
    irCall(callee, type, valueArgumentsCount = if (argument != null) 1 else 0, typeArgumentsCount = 0).apply {
        this.dispatchReceiver = dispatchReceiver
        if (argument != null)
            putValueArgument(0, argument)
    }

fun IrBuilderWithScope.typeOperator(
    resultType: IrType,
    argument: IrExpression,
    typeOperator: IrTypeOperator,
    typeOperand: IrType
) =
    IrTypeOperatorCallImpl(startOffset, endOffset, resultType, typeOperator, typeOperand, argument)

fun IrBuilderWithScope.irIs(argument: IrExpression, type: IrType) =
    typeOperator(context.irBuiltIns.booleanType, argument, IrTypeOperator.INSTANCEOF, type)

fun IrBuilderWithScope.irNotIs(argument: IrExpression, type: IrType) =
    typeOperator(context.irBuiltIns.booleanType, argument, IrTypeOperator.NOT_INSTANCEOF, type)

fun IrBuilderWithScope.irAs(argument: IrExpression, type: IrType) =
    IrTypeOperatorCallImpl(startOffset, endOffset, type, IrTypeOperator.CAST, type, argument)

fun IrBuilderWithScope.irImplicitCast(argument: IrExpression, type: IrType) =
    IrTypeOperatorCallImpl(startOffset, endOffset, type, IrTypeOperator.IMPLICIT_CAST, type, argument)

fun IrBuilderWithScope.irReinterpretCast(argument: IrExpression, type: IrType) =
    IrTypeOperatorCallImpl(startOffset, endOffset, type, IrTypeOperator.REINTERPRET_CAST, type, argument)

fun IrBuilderWithScope.irInt(value: Int) =
    IrConstImpl.int(startOffset, endOffset, context.irBuiltIns.intType, value)

fun IrBuilderWithScope.irLong(value: Long) =
    IrConstImpl.long(startOffset, endOffset, context.irBuiltIns.longType, value)

fun IrBuilderWithScope.irChar(value: Char) =
    IrConstImpl.char(startOffset, endOffset, context.irBuiltIns.charType, value)

fun IrBuilderWithScope.irString(value: String) =
    IrConstImpl.string(startOffset, endOffset, context.irBuiltIns.stringType, value)

fun IrBuilderWithScope.irConcat() =
    IrStringConcatenationImpl(startOffset, endOffset, context.irBuiltIns.stringType)


inline fun IrBuilderWithScope.irBlock(
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    origin: IrStatementOrigin? = null,
    resultType: IrType? = null,
    body: IrBlockBuilder.() -> Unit
): IrContainerExpression =
    IrBlockBuilder(
        context, scope,
        startOffset,
        endOffset,
        origin, resultType
    ).block(body)

inline fun IrBuilderWithScope.irComposite(
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    origin: IrStatementOrigin? = null,
    resultType: IrType? = null,
    body: IrBlockBuilder.() -> Unit
): IrExpression =
    IrBlockBuilder(
        context, scope,
        startOffset,
        endOffset,
        origin, resultType, true
    ).block(body)

inline fun IrBuilderWithScope.irBlockBody(
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    body: IrBlockBodyBuilder.() -> Unit
): IrBlockBody =
    IrBlockBodyBuilder(
        context, scope,
        startOffset,
        endOffset
    ).blockBody(body)

