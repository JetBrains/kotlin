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

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.types.KotlinType

interface IrMemberAccessExpression : IrExpression {
    var dispatchReceiver: IrExpression?
    var extensionReceiver: IrExpression?

    val descriptor: CallableDescriptor
    val origin: IrStatementOrigin?

    // NB `typeParameterDescriptor` should be taken from `descriptor.original`
    fun getTypeArgument(typeParameterDescriptor: TypeParameterDescriptor): KotlinType?

    fun getValueArgument(index: Int): IrExpression?
    fun putValueArgument(index: Int, valueArgument: IrExpression?)
    fun removeValueArgument(index: Int)
}

fun IrMemberAccessExpression.getTypeArgumentOrDefault(typeParameterDescriptor: TypeParameterDescriptor) =
    getTypeArgument(typeParameterDescriptor) ?: typeParameterDescriptor.defaultType

interface IrFunctionAccessExpression : IrMemberAccessExpression, IrDeclarationReference {
    override val descriptor: FunctionDescriptor
    override val symbol: IrFunctionSymbol
}

fun IrMemberAccessExpression.getValueArgument(valueParameterDescriptor: ValueParameterDescriptor) =
    getValueArgument(valueParameterDescriptor.index)

fun IrMemberAccessExpression.putValueArgument(valueParameterDescriptor: ValueParameterDescriptor, valueArgument: IrExpression?) {
    putValueArgument(valueParameterDescriptor.index, valueArgument)
}

fun IrMemberAccessExpression.removeValueArgument(valueParameterDescriptor: ValueParameterDescriptor) {
    removeValueArgument(valueParameterDescriptor.index)
}

inline fun <T : IrMemberAccessExpression> T.mapValueParameters(transform: (ValueParameterDescriptor) -> IrExpression?): T {
    descriptor.valueParameters.forEach {
        putValueArgument(it.index, transform(it))
    }
    return this
}

inline fun <T : IrMemberAccessExpression> T.mapValueParametersIndexed(transform: (Int, ValueParameterDescriptor) -> IrExpression?): T {
    descriptor.valueParameters.forEach {
        putValueArgument(it.index, transform(it.index, it))
    }
    return this
}

