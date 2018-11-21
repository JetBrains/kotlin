/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.assertedCast


inline fun IrBuilderWithScope.irLet(
    value: IrExpression,
    origin: IrStatementOrigin? = null,
    nameHint: String? = null,
    body: (VariableDescriptor) -> IrExpression
): IrExpression {
    val irTemporary = scope.createTemporaryVariable(value, nameHint)
    val irResult = body(irTemporary.descriptor)
    val irBlock = IrBlockImpl(startOffset, endOffset, irResult.type, origin)
    irBlock.statements.add(irTemporary)
    irBlock.statements.add(irResult)
    return irBlock
}

inline fun IrBuilderWithScope.irLetS(
    value: IrExpression,
    origin: IrStatementOrigin? = null,
    nameHint: String? = null,
    body: (IrValueSymbol) -> IrExpression
): IrExpression {
    val irTemporary = scope.createTemporaryVariable(value, nameHint)
    val irResult = body(irTemporary.symbol)
    val irBlock = IrBlockImpl(startOffset, endOffset, irResult.type, origin)
    irBlock.statements.add(irTemporary)
    irBlock.statements.add(irResult)
    return irBlock
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

fun <T : IrElement> IrStatementsBuilder<T>.defineTemporary(value: IrExpression, nameHint: String? = null): VariableDescriptor {
    val temporary = scope.createTemporaryVariable(value, nameHint)
    +temporary
    return temporary.descriptor
}

fun <T : IrElement> IrStatementsBuilder<T>.irTemporaryVar(
    value: IrExpression,
    nameHint: String? = null,
    typeHint: KotlinType? = null,
    parent: IrDeclarationParent? = null
): IrVariable {
    val temporary = scope.createTemporaryVariable(value, nameHint, isMutable = true, type = typeHint)
    parent?.let { temporary.parent = it }
    +temporary
    return temporary
}


fun <T : IrElement> IrStatementsBuilder<T>.defineTemporaryVar(value: IrExpression, nameHint: String? = null): VariableDescriptor {
    val temporary = scope.createTemporaryVariable(value, nameHint, isMutable = true)
    +temporary
    return temporary.descriptor
}

fun IrBuilderWithScope.irExprBody(value: IrExpression) =
    IrExpressionBodyImpl(startOffset, endOffset, value)

fun IrBuilderWithScope.irWhen(type: IrType, branches: List<IrBranch>) =
    IrWhenImpl(startOffset, endOffset, type, null, branches)

fun IrBuilderWithScope.irReturn(value: IrExpression) =
    IrReturnImpl(
        startOffset, endOffset,
        context.irBuiltIns.nothingType,
        scope.scopeOwnerSymbol.assertedCast<IrReturnTargetSymbol> {
            "Function scope expected: ${scope.scopeOwner}"
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

fun IrBuilderWithScope.irIfThen(type: IrType, condition: IrExpression, thenPart: IrExpression) =
    IrIfThenElseImpl(startOffset, endOffset, type).apply {
        branches.add(IrBranchImpl(startOffset, endOffset, condition, thenPart))
    }

fun IrBuilderWithScope.irIfThenElse(type: IrType, condition: IrExpression, thenPart: IrExpression, elsePart: IrExpression) =
    IrIfThenElseImpl(startOffset, endOffset, type).apply {
        branches.add(IrBranchImpl(startOffset, endOffset, condition, thenPart))
        branches.add(irElseBranch(elsePart))
    }

fun IrBuilderWithScope.irIfThenMaybeElse(type: IrType, condition: IrExpression, thenPart: IrExpression, elsePart: IrExpression?) =
    if (elsePart != null)
        irIfThenElse(type, condition, thenPart, elsePart)
    else
        irIfThen(type, condition, thenPart)

fun IrBuilderWithScope.irIfNull(type: IrType, subject: IrExpression, thenPart: IrExpression, elsePart: IrExpression) =
    irIfThenElse(type, irEqualsNull(subject), thenPart, elsePart)

fun IrBuilderWithScope.irThrowNpe(origin: IrStatementOrigin? = null) =
    IrNullaryPrimitiveImpl(startOffset, endOffset, context.irBuiltIns.nothingType, origin, context.irBuiltIns.throwNpeSymbol)

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
    IrSetFieldImpl(startOffset, endOffset, field.symbol, receiver, value, field.type)

fun IrBuilderWithScope.irGetObjectValue(type: IrType, classSymbol: IrClassSymbol) =
    IrGetObjectValueImpl(startOffset, endOffset, type, classSymbol)

fun IrBuilderWithScope.irEqeqeq(arg1: IrExpression, arg2: IrExpression) =
    context.eqeqeq(startOffset, endOffset, arg1, arg2)

fun IrBuilderWithScope.irNull() =
    IrConstImpl.constNull(startOffset, endOffset, context.irBuiltIns.nothingNType)

fun IrBuilderWithScope.irEqualsNull(argument: IrExpression) =
    primitiveOp2(
        startOffset, endOffset, context.irBuiltIns.eqeqSymbol, IrStatementOrigin.EQEQ,
        argument, irNull()
    )

fun IrBuilderWithScope.irEquals(arg1: IrExpression, arg2: IrExpression, origin: IrStatementOrigin = IrStatementOrigin.EQEQ) =
    primitiveOp2(
        startOffset, endOffset, context.irBuiltIns.eqeqSymbol, origin,
        arg1, arg2
    )

fun IrBuilderWithScope.irNotEquals(arg1: IrExpression, arg2: IrExpression) =
    primitiveOp1(
        startOffset, endOffset, context.irBuiltIns.booleanNotSymbol, IrStatementOrigin.EXCLEQ,
        irEquals(arg1, arg2, origin = IrStatementOrigin.EXCLEQ)
    )

fun IrBuilderWithScope.irGet(type: IrType, receiver: IrExpression, getterSymbol: IrFunctionSymbol): IrCall =
    IrGetterCallImpl(
        startOffset, endOffset,
        type,
        getterSymbol, getterSymbol.descriptor,
        typeArgumentsCount = 0,
        dispatchReceiver = receiver,
        extensionReceiver = null,
        origin = IrStatementOrigin.GET_PROPERTY
    )

fun IrBuilderWithScope.irCall(callee: IrFunctionSymbol, type: IrType, typeArguments: List<IrType> = emptyList()): IrCall =
    IrCallImpl(startOffset, endOffset, type, callee, callee.descriptor).apply {
        typeArguments.forEachIndexed { index, irType ->
            this.putTypeArgument(index, irType)
        }
    }

fun IrBuilderWithScope.irCall(callee: IrFunctionSymbol): IrCall =
    IrCallImpl(startOffset, endOffset, callee.owner.returnType, callee, callee.descriptor)

fun IrBuilderWithScope.irCall(callee: IrFunctionSymbol, descriptor: FunctionDescriptor, type: IrType): IrCall =
    IrCallImpl(startOffset, endOffset, type, callee, descriptor)

fun IrBuilderWithScope.irCall(callee: IrFunction): IrCall =
    irCall(callee.symbol, callee.descriptor, callee.returnType)

fun IrBuilderWithScope.irCall(callee: IrFunction, origin: IrStatementOrigin): IrCall =
    IrCallImpl(startOffset, endOffset, callee.returnType, callee.symbol, callee.descriptor, origin)

fun IrBuilderWithScope.irDelegatingConstructorCall(callee: IrConstructor): IrDelegatingConstructorCall =
    IrDelegatingConstructorCallImpl(startOffset, endOffset, callee.returnType, callee.symbol, callee.descriptor,
            callee.parentAsClass.typeParameters.size, callee.valueParameters.size)

fun IrBuilderWithScope.irCallOp(
    callee: IrFunctionSymbol,
    type: IrType,
    dispatchReceiver: IrExpression,
    argument: IrExpression
): IrCall =
    irCall(callee, type).apply {
        this.dispatchReceiver = dispatchReceiver
        putValueArgument(0, argument)
    }

fun IrBuilderWithScope.typeOperator(
    resultType: IrType,
    argument: IrExpression,
    typeOperator: IrTypeOperator,
    typeOperand: IrType
) =
    IrTypeOperatorCallImpl(startOffset, endOffset, resultType, typeOperator, typeOperand, typeOperand.classifierOrFail, argument)

fun IrBuilderWithScope.irIs(argument: IrExpression, type: IrType) =
    typeOperator(context.irBuiltIns.booleanType, argument, IrTypeOperator.INSTANCEOF, type)

fun IrBuilderWithScope.irNotIs(argument: IrExpression, type: IrType) =
    typeOperator(context.irBuiltIns.booleanType, argument, IrTypeOperator.NOT_INSTANCEOF, type)

fun IrBuilderWithScope.irAs(argument: IrExpression, type: IrType) =
    IrTypeOperatorCallImpl(startOffset, endOffset, type, IrTypeOperator.CAST, type, type.classifierOrFail, argument)

fun IrBuilderWithScope.irImplicitCast(argument: IrExpression, type: IrType) =
    IrTypeOperatorCallImpl(startOffset, endOffset, type, IrTypeOperator.IMPLICIT_CAST, type, type.classifierOrFail, argument)

fun IrBuilderWithScope.irInt(value: Int) =
    IrConstImpl.int(startOffset, endOffset, context.irBuiltIns.intType, value)

fun IrBuilderWithScope.irString(value: String) =
    IrConstImpl.string(startOffset, endOffset, context.irBuiltIns.stringType, value)

fun IrBuilderWithScope.irConcat() =
    IrStringConcatenationImpl(startOffset, endOffset, context.irBuiltIns.stringType)


fun IrBuilderWithScope.irSetField(receiver: IrExpression, irField: IrField, value: IrExpression): IrExpression =
    IrSetFieldImpl(
        startOffset,
        endOffset,
        irField.symbol,
        receiver = receiver,
        value = value,
        type = context.irBuiltIns.unitType
    )
