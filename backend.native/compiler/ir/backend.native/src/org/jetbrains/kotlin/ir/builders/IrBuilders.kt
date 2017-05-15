/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor

inline fun IrBuilderWithScope.irLetSequence(
        value: IrExpression,
        nameHint: String? = null,
        startOffset: Int = this.startOffset,
        endOffset: Int = this.endOffset,
        origin: IrStatementOrigin? = null,
        resultType: KotlinType? = null,
        body: IrBlockBuilder.(VariableDescriptor) -> Unit
): IrExpression = irBlock(startOffset, endOffset, origin, resultType) {
    val irTemporary = defineTemporary(value, nameHint)
    this.body(irTemporary)
}

fun IrBuilderWithScope.irWhile(origin: IrStatementOrigin? = null) =
        IrWhileLoopImpl(startOffset, endOffset, context.builtIns.unitType, origin)

fun IrBuilderWithScope.irBreak(loop: IrLoop) =
        IrBreakImpl(startOffset, endOffset, context.builtIns.nothingType, loop)

fun IrBuilderWithScope.irContinue(loop: IrLoop) =
        IrContinueImpl(startOffset, endOffset, context.builtIns.nothingType, loop)

fun IrBuilderWithScope.irTrue() = IrConstImpl.boolean(startOffset, endOffset, context.builtIns.booleanType, true)

@Deprecated("Creates unbound symbol")
fun IrBuilderWithScope.irGet(value: ValueDescriptor) =
        IrGetValueImpl(startOffset, endOffset, value)

@Deprecated("Creates unbound symbol")
fun IrBuilderWithScope.irGet(receiver: IrExpression?, property: PropertyDescriptor): IrCall =
        IrCallImpl(startOffset, endOffset, property.getter!!).apply {
            dispatchReceiver = receiver
        }

@Deprecated("Creates unbound symbol")
fun IrBuilderWithScope.irThis() =
        scope.classOwner().let { classOwner ->
            IrGetValueImpl(startOffset, endOffset, classOwner.thisAsReceiverParameter)
        }

@Deprecated("Creates unbound symbol")
fun IrBuilderWithScope.irSetVar(variable: VariableDescriptor, value: IrExpression) =
        IrSetVariableImpl(startOffset, endOffset, variable, value, IrStatementOrigin.EQ)

@Deprecated("Creates unbound symbol")
fun IrBuilderWithScope.irCall(descriptor: FunctionDescriptor): IrCallImpl {
    return IrCallImpl(this.startOffset, this.endOffset, descriptor)
}

fun IrBuilderWithScope.irCall(symbol: IrFunctionSymbol): IrCallImpl {
    return IrCallImpl(this.startOffset, this.endOffset, symbol)
}

@Deprecated("Creates unbound symbol")
fun IrBuilderWithScope.irCall(
        callee: FunctionDescriptor,
        typeArguments: Map<TypeParameterDescriptor, KotlinType>
): IrCallImpl {
    val substitutionContext = typeArguments.map { (typeParameter, typeArgument) ->
        typeParameter.typeConstructor to TypeProjectionImpl(typeArgument)
    }.toMap()

    val substitutor = TypeSubstitutor.create(substitutionContext)
    val substitutedCallee = callee.substitute(substitutor)!!

    return IrCallImpl(this.startOffset, this.endOffset, substitutedCallee, typeArguments)
}