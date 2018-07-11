/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun IrType.withHasQuestionMark(hasQuestionMark: Boolean): IrType =
    when (this) {
        is IrSimpleType ->
            if (this.hasQuestionMark == hasQuestionMark)
                this
            else
                IrSimpleTypeImpl(
                    makeKotlinType(classifier, arguments, hasQuestionMark),
                    classifier,
                    hasQuestionMark,
                    arguments,
                    annotations
                )
        else -> this
    }

val IrType.classifierOrFail: IrClassifierSymbol
    get() = cast<IrSimpleType>().classifier

val IrType.classifierOrNull: IrClassifierSymbol?
    get() = safeAs<IrSimpleType>()?.classifier

fun IrType.makeNotNull() =
    if (this is IrSimpleType && this.hasQuestionMark)
        IrSimpleTypeImpl(
            makeKotlinType(classifier, arguments, false),
            classifier,
            false,
            arguments,
            annotations,
            Variance.INVARIANT
        )
    else
        this

fun IrType.makeNullable() =
    if (this is IrSimpleType && !this.hasQuestionMark)
        IrSimpleTypeImpl(
            makeKotlinType(classifier, arguments, true),
            classifier,
            true,
            arguments,
            annotations,
            Variance.INVARIANT
        )
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

fun ClassifierDescriptor.toIrType(hasQuestionMark: Boolean = false): IrType {
    val symbol = getSymbol()
    return IrSimpleTypeImpl(defaultType, symbol, hasQuestionMark, listOf(), listOf())
}

fun KotlinType.toIrType(): IrType? {
    if (isDynamic()) return IrDynamicTypeImpl(this, listOf(), Variance.INVARIANT)

    val symbol = constructor.declarationDescriptor?.getSymbol() ?: return null

    val arguments = this.arguments.mapIndexed { i, projection ->
        when (projection) {
            is TypeProjectionImpl -> IrTypeProjectionImpl(projection.type.toIrType()!!, projection.projectionKind)
            is StarProjectionImpl -> IrStarProjectionImpl
            else -> error(projection)
        }
    }

    // TODO
    val annotations = listOf()
    return IrSimpleTypeImpl(this, symbol, isMarkedNullable, arguments, annotations)
}

private fun ClassifierDescriptor.getSymbol(): IrClassifierSymbol = when (this) {
    is ClassDescriptor -> IrClassSymbolImpl(this)
    is TypeParameterDescriptor -> IrTypeParameterSymbolImpl(this)
    else -> TODO()
}