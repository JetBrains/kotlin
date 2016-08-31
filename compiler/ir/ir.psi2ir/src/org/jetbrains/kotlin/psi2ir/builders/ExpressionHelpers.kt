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

package org.jetbrains.kotlin.psi2ir.builders

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi2ir.generators.eqeqeq
import org.jetbrains.kotlin.psi2ir.generators.primitiveOp1
import org.jetbrains.kotlin.psi2ir.generators.primitiveOp2
import org.jetbrains.kotlin.types.KotlinType


inline fun IrBuilderWithScope.irLet(
        value: IrExpression,
        operator: IrOperator? = null,
        nameHint: String? = null,
        body: (VariableDescriptor) -> IrExpression
): IrExpression {
    val irTemporary = scope.createTemporaryVariable(value, nameHint)
    val irResult = body(irTemporary.descriptor)
    val irBlock = IrBlockImpl(startOffset, endOffset, irResult.type, operator)
    irBlock.addStatement(irTemporary)
    irBlock.addStatement(irResult)
    return irBlock
}

fun <T : IrElement> IrStatementsBuilder<T>.defineTemporary(value: IrExpression, nameHint: String? = null): VariableDescriptor {
    val temporary = scope.createTemporaryVariable(value, nameHint)
    +temporary
    return temporary.descriptor
}

fun IrBuilderWithScope.irReturn(value: IrExpression) =
        IrReturnImpl(startOffset, endOffset, context.builtIns.nothingType, scope.assertCastOwner(), value)

fun IrBuilderWithScope.irReturnTrue() =
        irReturn(IrConstImpl(startOffset, endOffset, context.builtIns.booleanType, IrConstKind.Boolean, true))

fun IrBuilderWithScope.irReturnFalse() =
        irReturn(IrConstImpl(startOffset, endOffset, context.builtIns.booleanType, IrConstKind.Boolean, false))

fun IrBuilderWithScope.irIfThenElse(type: KotlinType, condition: IrExpression, thenPart: IrExpression, elsePart: IrExpression) =
        IrIfThenElseImpl(startOffset, endOffset, type, condition, thenPart, elsePart)

fun IrBuilderWithScope.irIfNull(type: KotlinType, subject: IrExpression, thenPart: IrExpression, elsePart: IrExpression) =
        irIfThenElse(type, irEqualsNull(subject), thenPart, elsePart)

fun IrBuilderWithScope.irThrowNpe(operator: IrOperator) =
        IrNullaryPrimitiveImpl(startOffset, endOffset, operator, context.irBuiltIns.throwNpe)

fun IrBuilderWithScope.irIfThenReturnTrue(condition: IrExpression) =
        IrIfThenElseImpl(startOffset, endOffset, context.builtIns.unitType, condition, irReturnTrue())

fun IrBuilderWithScope.irIfThenReturnFalse(condition: IrExpression) =
        IrIfThenElseImpl(startOffset, endOffset, context.builtIns.unitType, condition, irReturnFalse())

fun IrBuilderWithScope.irThis() =
        scope.classOwner().let { classOwner ->
            IrThisReferenceImpl(startOffset, endOffset, classOwner.defaultType, classOwner)
        }


fun IrBuilderWithScope.irGet(variable: VariableDescriptor) =
        IrGetVariableImpl(startOffset, endOffset, variable)

fun IrBuilderWithScope.irSetVar(variable: VariableDescriptor, value: IrExpression) =
        IrSetVariableImpl(startOffset, endOffset, variable, value, IrOperator.EQ)

fun IrBuilderWithScope.irOther() =
        irGet(scope.functionOwner().valueParameters.single())

fun IrBuilderWithScope.irEqeqeq(arg1: IrExpression, arg2: IrExpression) =
        context.eqeqeq(startOffset, endOffset, arg1, arg2)

fun IrBuilderWithScope.irNull() =
        IrConstImpl.constNull(startOffset, endOffset, context.builtIns.nullableNothingType)

fun IrBuilderWithScope.irEqualsNull(argument: IrExpression) =
        primitiveOp2(startOffset, endOffset, context.irBuiltIns.eqeq, IrOperator.EQEQ,
                     argument, irNull())

fun IrBuilderWithScope.irNotEquals(arg1: IrExpression, arg2: IrExpression) =
        primitiveOp1(startOffset, endOffset, context.irBuiltIns.booleanNot, IrOperator.EXCLEQ,
                     primitiveOp2(startOffset, endOffset, context.irBuiltIns.eqeq, IrOperator.EXCLEQ,
                                  arg1, arg2))

fun IrBuilderWithScope.irGet(receiver: IrExpression, property: PropertyDescriptor): IrExpression =
        IrGetterCallImpl(startOffset, endOffset, property.getter!!, receiver, null, IrOperator.GET_PROPERTY)

fun IrBuilderWithScope.irCall(callee: CallableDescriptor) =
        IrCallImpl(startOffset, endOffset, callee.returnType!!, callee)

fun IrBuilderWithScope.irCallOp(callee: CallableDescriptor, dispatchReceiver: IrExpression, argument: IrExpression) =
        IrCallImpl(startOffset, endOffset, callee.returnType!!, callee).apply {
            this.dispatchReceiver = dispatchReceiver
            putArgument(0, argument)
        }

fun IrBuilderWithScope.irNotIs(argument: IrExpression, type: KotlinType) =
        IrTypeOperatorCallImpl(startOffset, endOffset, context.builtIns.booleanType, IrTypeOperator.NOT_INSTANCEOF, type, argument)

fun IrBuilderWithScope.irAs(argument: IrExpression, type: KotlinType) =
        IrTypeOperatorCallImpl(startOffset, endOffset, type, IrTypeOperator.CAST, type, argument)

fun IrBuilderWithScope.irInt(value: Int) =
        IrConstImpl.int(startOffset, endOffset, context.builtIns.intType, value)

fun IrBuilderWithScope.irString(value: String) =
        IrConstImpl.string(startOffset, endOffset, context.builtIns.stringType, value)

fun IrBuilderWithScope.irConcat() =
        IrStringConcatenationImpl(startOffset, endOffset, context.builtIns.stringType)