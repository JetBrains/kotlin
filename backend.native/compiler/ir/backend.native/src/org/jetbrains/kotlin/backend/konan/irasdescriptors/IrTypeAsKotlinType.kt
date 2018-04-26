/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.konan.irasdescriptors

import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.getFunctionalClassKind
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

val IrClassifierSymbol.typeWithoutArguments: IrType
    get() = when (this) {
        is IrClassSymbol -> {
            require(this.descriptor.declaredTypeParameters.isEmpty())
            this.typeWith(arguments = emptyList())
        }
        is IrTypeParameterSymbol -> this.defaultType
        else -> error(this)
    }

val IrClassifierSymbol.typeWithStarProjections
    get() = when (this) {
        is IrClassSymbol -> createType(
                hasQuestionMark = false,
                arguments = this.descriptor.declaredTypeParameters.map { IrStarProjectionImpl }
        )
        is IrTypeParameterSymbol -> this.defaultType
        else -> error(this)
    }

val IrTypeParameterSymbol.defaultType: IrType get() =  IrSimpleTypeImpl(
        this,
        false,
        emptyList(),
        emptyList()
)
val IrTypeParameter.defaultType: IrType get() = this.symbol.defaultType

fun IrClassifierSymbol.typeWith(vararg arguments: IrType): IrSimpleType = typeWith(arguments.toList())

fun IrClassifierSymbol.typeWith(arguments: List<IrType>): IrSimpleType =
        IrSimpleTypeImpl(
                this,
                false,
                arguments.map { makeTypeProjection(it, Variance.INVARIANT) },
                emptyList()
        )

fun IrClass.typeWith(arguments: List<IrType>) = this.symbol.typeWith(arguments)

fun IrClassSymbol.createType(hasQuestionMark: Boolean, arguments: List<IrTypeArgument>): IrSimpleType =
        IrSimpleTypeImpl(
                this,
                hasQuestionMark,
                arguments,
                emptyList()
        )

fun IrType.getClass(): IrClass? =
        (this.classifierOrNull as? IrClassSymbol)?.owner

fun IrType.makeNullableAsSpecified(nullable: Boolean): IrType =
        if (nullable) this.makeNullable() else this.makeNotNull()

fun IrType.containsNull(): Boolean = if (this is IrSimpleType) {
    if (this.hasQuestionMark) {
        true
    } else {
        val classifier = this.classifier
        when (classifier) {
            is IrClassSymbol -> false
            is IrTypeParameterSymbol -> classifier.owner.superTypes.any { it.containsNull() }
            else -> error(classifier)
        }
    }
} else {
    true
}

// TODO: get rid of these:
fun IrType.isSubtypeOf(other: KotlinType): Boolean = this.toKotlinType().isSubtypeOf(other)
fun IrType.isSubtypeOf(other: IrType): Boolean = this.isSubtypeOf(other.toKotlinType())

val IrType.isFunctionOrKFunctionType: Boolean
    get() = when (this) {
        is IrSimpleType -> {
            val kind = classifier.descriptor.getFunctionalClassKind()
            kind == FunctionClassDescriptor.Kind.Function || kind == FunctionClassDescriptor.Kind.KFunction
        }
        else -> false
    }

internal tailrec fun IrType.getErasedTypeClass(): IrClassSymbol {
    val classifier = this.classifierOrFail
    return when (classifier) {
        is IrClassSymbol -> classifier
        is IrTypeParameterSymbol -> classifier.owner.superTypes.first().getErasedTypeClass()
        else -> error(classifier)
    }
}
