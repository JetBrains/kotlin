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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.types.KotlinType

interface IrMemberAccessExpression : IrExpression {
    var dispatchReceiver: IrExpression?
    var extensionReceiver: IrExpression?

    val descriptor: CallableDescriptor
    val origin: IrStatementOrigin?

    val typeArgumentsCount: Int
    fun getTypeArgument(index: Int): IrType?
    fun putTypeArgument(index: Int, type: IrType?)

    val valueArgumentsCount: Int
    fun getValueArgument(index: Int): IrExpression?
    fun putValueArgument(index: Int, valueArgument: IrExpression?)
    fun removeValueArgument(index: Int)
}

fun IrMemberAccessExpression.getTypeArgument(typeParameterDescriptor: TypeParameterDescriptor): IrType? =
    getTypeArgument(typeParameterDescriptor.index)

fun IrMemberAccessExpression.copyTypeArgumentsFrom(other: IrMemberAccessExpression) {
    assert(typeArgumentsCount == other.typeArgumentsCount) {
        "Mismatching type arguments: $typeArgumentsCount vs ${other.typeArgumentsCount} "
    }
    for (i in 0 until typeArgumentsCount) {
        putTypeArgument(i, other.getTypeArgument(i))
    }
}

inline fun IrMemberAccessExpression.putTypeArguments(
    typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
    toIrType: (KotlinType) -> IrType
) {
    if (typeArguments == null) return
    for ((typeParameter, typeArgument) in typeArguments) {
        putTypeArgument(typeParameter.index, toIrType(typeArgument))
    }
}

val CallableDescriptor.typeParametersCount: Int
    get() =
        when (this) {
            is PropertyAccessorDescriptor -> correspondingProperty.typeParameters.size
            else -> typeParameters.size
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

inline fun <T : IrMemberAccessExpression> T.mapTypeParameters(transform: (TypeParameterDescriptor) -> IrType) : T =
    apply {
        descriptor.typeParameters.forEach {
            putTypeArgument(it.index, transform(it))
        }
    }

inline fun <T : IrMemberAccessExpression> T.mapValueParameters(transform: (ValueParameterDescriptor) -> IrExpression?): T =
    apply {
        descriptor.valueParameters.forEach {
            putValueArgument(it.index, transform(it))
        }
    }

inline fun <T : IrMemberAccessExpression> T.mapValueParametersIndexed(transform: (Int, ValueParameterDescriptor) -> IrExpression?): T =
    apply {
        descriptor.valueParameters.forEach {
            putValueArgument(it.index, transform(it.index, it))
        }
    }

