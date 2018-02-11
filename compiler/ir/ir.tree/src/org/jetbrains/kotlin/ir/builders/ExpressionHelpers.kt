/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir.builders

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
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


fun <T : IrElement> IrStatementsBuilder<T>.irTemporary(value: IrExpression, nameHint: String? = null): IrVariable {
    val temporary = scope.createTemporaryVariable(value, nameHint)
    +temporary
    return temporary
}

fun <T : IrElement> IrStatementsBuilder<T>.defineTemporary(value: IrExpression, nameHint: String? = null): VariableDescriptor {
    val temporary = scope.createTemporaryVariable(value, nameHint)
    +temporary
    return temporary.descriptor
}

fun <T : IrElement> IrStatementsBuilder<T>.irTemporaryVar(value: IrExpression, nameHint: String? = null): IrVariable {
    val temporary = scope.createTemporaryVariable(value, nameHint, isMutable = true)
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

fun IrBuilderWithScope.irReturn(value: IrExpression) =
    IrReturnImpl(
        startOffset, endOffset, context.builtIns.nothingType,
        scope.scopeOwnerSymbol.assertedCast<IrFunctionSymbol> {
            "Function scope expected: ${scope.scopeOwner}"
        },
        value
    )

fun IrBuilderWithScope.irReturnTrue() =
    irReturn(IrConstImpl(startOffset, endOffset, context.builtIns.booleanType, IrConstKind.Boolean, true))

fun IrBuilderWithScope.irReturnFalse() =
    irReturn(IrConstImpl(startOffset, endOffset, context.builtIns.booleanType, IrConstKind.Boolean, false))

fun IrBuilderWithScope.irIfThenElse(type: KotlinType, condition: IrExpression, thenPart: IrExpression, elsePart: IrExpression) =
    IrIfThenElseImpl(startOffset, endOffset, type, condition, thenPart, elsePart)

fun IrBuilderWithScope.irIfNull(type: KotlinType, subject: IrExpression, thenPart: IrExpression, elsePart: IrExpression) =
    irIfThenElse(type, irEqualsNull(subject), thenPart, elsePart)

fun IrBuilderWithScope.irThrowNpe(origin: IrStatementOrigin) =
    IrNullaryPrimitiveImpl(startOffset, endOffset, origin, context.irBuiltIns.throwNpeSymbol)

fun IrBuilderWithScope.irIfThenReturnTrue(condition: IrExpression) =
    IrIfThenElseImpl(startOffset, endOffset, context.builtIns.unitType, condition, irReturnTrue())

fun IrBuilderWithScope.irIfThenReturnFalse(condition: IrExpression) =
    IrIfThenElseImpl(startOffset, endOffset, context.builtIns.unitType, condition, irReturnFalse())

fun IrBuilderWithScope.irGet(variable: IrValueSymbol) =
    IrGetValueImpl(startOffset, endOffset, variable)

fun IrBuilderWithScope.irSetVar(variable: IrVariableSymbol, value: IrExpression) =
    IrSetVariableImpl(startOffset, endOffset, variable, value, IrStatementOrigin.EQ)

fun IrBuilderWithScope.irEqeqeq(arg1: IrExpression, arg2: IrExpression) =
    context.eqeqeq(startOffset, endOffset, arg1, arg2)

fun IrBuilderWithScope.irNull() =
    IrConstImpl.constNull(startOffset, endOffset, context.builtIns.nullableNothingType)

fun IrBuilderWithScope.irEqualsNull(argument: IrExpression) =
    primitiveOp2(
        startOffset, endOffset, context.irBuiltIns.eqeqSymbol, IrStatementOrigin.EQEQ,
        argument, irNull()
    )

fun IrBuilderWithScope.irNotEquals(arg1: IrExpression, arg2: IrExpression) =
    primitiveOp1(
        startOffset, endOffset, context.irBuiltIns.booleanNotSymbol, IrStatementOrigin.EXCLEQ,
        primitiveOp2(
            startOffset, endOffset, context.irBuiltIns.eqeqSymbol, IrStatementOrigin.EXCLEQ,
            arg1, arg2
        )
    )

fun IrBuilderWithScope.irGet(receiver: IrExpression, getterSymbol: IrFunctionSymbol): IrCall =
    IrGetterCallImpl(startOffset, endOffset, getterSymbol, getterSymbol.descriptor, null, receiver, null, IrStatementOrigin.GET_PROPERTY)

fun IrBuilderWithScope.irCall(callee: IrFunctionSymbol, type: KotlinType): IrCall =
    IrCallImpl(startOffset, endOffset, type, callee, callee.descriptor, null)

fun IrBuilderWithScope.irCall(callee: IrFunctionSymbol): IrCall =
    irCall(callee, callee.descriptor.returnType!!)

fun IrBuilderWithScope.irCallOp(callee: IrFunctionSymbol, dispatchReceiver: IrExpression, argument: IrExpression): IrCall =
    irCall(callee, callee.descriptor.returnType!!).apply {
        this.dispatchReceiver = dispatchReceiver
        putValueArgument(0, argument)
    }

fun IrBuilderWithScope.irCallOp(
    callee: IrFunctionSymbol,
    type: KotlinType,
    dispatchReceiver: IrExpression,
    argument: IrExpression
): IrCall =
    irCall(callee, type).apply {
        this.dispatchReceiver = dispatchReceiver
        putValueArgument(0, argument)
    }

fun IrBuilderWithScope.irIs(argument: IrExpression, type: KotlinType) =
    IrTypeOperatorCallImpl(startOffset, endOffset, context.builtIns.booleanType, IrTypeOperator.INSTANCEOF, type, argument)

fun IrBuilderWithScope.irNotIs(argument: IrExpression, type: KotlinType) =
    IrTypeOperatorCallImpl(startOffset, endOffset, context.builtIns.booleanType, IrTypeOperator.NOT_INSTANCEOF, type, argument)

fun IrBuilderWithScope.irAs(argument: IrExpression, type: KotlinType) =
    IrTypeOperatorCallImpl(startOffset, endOffset, type, IrTypeOperator.CAST, type, argument)

fun IrBuilderWithScope.irImplicitCast(argument: IrExpression, type: KotlinType) =
    IrTypeOperatorCallImpl(startOffset, endOffset, type, IrTypeOperator.IMPLICIT_CAST, type, argument)

fun IrBuilderWithScope.irInt(value: Int) =
    IrConstImpl.int(startOffset, endOffset, context.builtIns.intType, value)

fun IrBuilderWithScope.irString(value: String) =
    IrConstImpl.string(startOffset, endOffset, context.builtIns.stringType, value)

fun IrBuilderWithScope.irConcat() =
    IrStringConcatenationImpl(startOffset, endOffset, context.builtIns.stringType)
