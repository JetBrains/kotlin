/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun IrType.withHasQuestionMark(newHasQuestionMark: Boolean): IrType =
    when (this) {
        is IrSimpleType ->
            if (this.hasQuestionMark == newHasQuestionMark)
                this
            else
                buildSimpleType {
                    hasQuestionMark = newHasQuestionMark
                    kotlinType = originalKotlinType?.run {
                        if (newHasQuestionMark) makeNullable() else makeNotNullable()
                    }
                }
        else -> this
    }

val IrType.classifierOrFail: IrClassifierSymbol
    get() = cast<IrSimpleType>().classifier

val IrType.classifierOrNull: IrClassifierSymbol?
    get() = safeAs<IrSimpleType>()?.classifier

val IrType.classOrNull: IrClassSymbol?
    get() = classifierOrNull as? IrClassSymbol

val IrTypeArgument.typeOrNull: IrType? get() = (this as? IrTypeProjection)?.type

fun IrType.makeNotNull() =
    if (this is IrSimpleType && this.hasQuestionMark) {
        buildSimpleType {
            kotlinType = originalKotlinType?.makeNotNullable()
            hasQuestionMark = false
        }
    } else
        this

fun IrType.makeNullable() =
    if (this is IrSimpleType && !this.hasQuestionMark)
        buildSimpleType {
            kotlinType = originalKotlinType?.makeNullable()
            hasQuestionMark = true
        }
    else
        this

@DescriptorBasedIr
fun IrType.toKotlinType(): KotlinType {
    originalKotlinType?.let {
        return it
    }

    return when (this) {
        is IrSimpleType -> makeKotlinType(classifier, arguments, hasQuestionMark)
        else -> TODO(toString())
    }
}

fun IrType.getClass(): IrClass? =
    classOrNull?.owner

fun IrClassSymbol.createType(hasQuestionMark: Boolean, arguments: List<IrTypeArgument>): IrSimpleType =
    IrSimpleTypeImpl(
        this,
        hasQuestionMark,
        arguments,
        emptyList()
    )

@DescriptorBasedIr
private fun makeKotlinType(
    classifier: IrClassifierSymbol,
    arguments: List<IrTypeArgument>,
    hasQuestionMark: Boolean
): SimpleType {
    val kotlinTypeArguments = arguments.mapIndexed { index, it ->
        when (it) {
            is IrTypeProjection -> TypeProjectionImpl(it.variance, it.type.toKotlinType())
            is IrStarProjection -> StarProjectionImpl((classifier.descriptor as ClassDescriptor).typeConstructor.parameters[index])
            else -> error(it)
        }
    }
    return classifier.descriptor.defaultType.replace(newArguments = kotlinTypeArguments).makeNullableAsSpecified(hasQuestionMark)
}

val IrClassifierSymbol.defaultType: IrType
    get() = when (this) {
        is IrClassSymbol -> owner.defaultType
        is IrTypeParameterSymbol -> owner.defaultType
        else -> error("Unexpected classifier symbol type $this")
    }

val IrTypeParameter.defaultType: IrType
    get() = IrSimpleTypeImpl(
        symbol,
        hasQuestionMark = false,
        arguments = emptyList(),
        annotations = emptyList()
    )

val IrClassSymbol.starProjectedType: IrSimpleType
    get() = IrSimpleTypeImpl(
        this,
        hasQuestionMark = false,
        arguments = owner.typeParameters.map { IrStarProjectionImpl },
        annotations = emptyList()
    )

fun IrClassifierSymbol.typeWithParameters(parameters: List<IrTypeParameter>): IrSimpleType =
    typeWith(parameters.map { it.defaultType })

fun IrClassifierSymbol.typeWith(vararg arguments: IrType): IrSimpleType = typeWith(arguments.toList())

fun IrClassifierSymbol.typeWith(arguments: List<IrType>): IrSimpleType =
    IrSimpleTypeImpl(
        this,
        false,
        arguments.map { makeTypeProjection(it, Variance.INVARIANT) },
        emptyList()
    )

fun IrClass.typeWith(arguments: List<IrType>) = this.symbol.typeWith(arguments)

fun IrClass.typeWith(vararg arguments: IrType) = this.symbol.typeWith(arguments.toList())
