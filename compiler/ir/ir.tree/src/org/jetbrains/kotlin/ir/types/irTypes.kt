/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedTypeParameterDescriptor
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.impl.*
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

val IrType.isNullable: Boolean
    get() = isMarkedNullable() || this is IrDynamicType ||
            classifierOrNull?.safeAs<IrTypeParameterSymbol>()?.owner?.superTypes?.all { it.isNullable } == true

fun IrType.makeNotNull(): IrType {
    if (!this.isNullable || this !is IrSimpleType) {
        // This is wrong for IrDynamicType, but there's nothing we can do about it.
        return this
    }

    val newClassifier = if (classifier is IrTypeParameterSymbol) {
        val descriptor = WrappedTypeParameterDescriptor()
        val symbol = IrTypeParameterSymbolImpl(descriptor)
        val newParameter = with(classifier.owner as IrTypeParameter) {
            IrTypeParameterImpl(startOffset, endOffset, origin, symbol, name, index, isReified, variance).also {
                descriptor.bind(it)
                it.parent = parent
                it.annotations.addAll(annotations)
                superTypes.mapTo(it.superTypes) { type -> type.makeNotNull() }
            }
        }
        newParameter.symbol
    } else {
        classifier
    }

    return IrSimpleTypeImpl(
        originalKotlinType?.makeNotNullable(),
        newClassifier,
        false,
        arguments,
        annotations
    )
}

fun IrType.makeNullable() =
    if (this is IrSimpleType && !this.hasQuestionMark)
        buildSimpleType {
            kotlinType = originalKotlinType?.makeNullable()
            hasQuestionMark = true
        }
    else
        this

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

val IrTypeParameter.defaultType: IrType
    get() = IrSimpleTypeImpl(
        symbol,
        hasQuestionMark = false,
        arguments = emptyList(),
        annotations = emptyList()
    )

fun IrClassifierSymbol.typeWith(vararg arguments: IrType): IrSimpleType = typeWith(arguments.toList())

fun IrClassifierSymbol.typeWith(arguments: List<IrType>): IrSimpleType =
    IrSimpleTypeImpl(
        this,
        false,
        arguments.map { makeTypeProjection(it, Variance.INVARIANT) },
        emptyList()
    )

fun IrClass.typeWith(arguments: List<IrType>) = this.symbol.typeWith(arguments)
