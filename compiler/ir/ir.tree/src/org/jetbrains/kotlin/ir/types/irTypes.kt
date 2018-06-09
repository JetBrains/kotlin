/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun IrType.withHasQuestionMark(hasQuestionMark: Boolean): IrType =
    when (this) {
        is IrSimpleType ->
            if (this.hasQuestionMark == hasQuestionMark)
                this
            else
                IrSimpleTypeImpl(originalKotlinType, classifier, hasQuestionMark, arguments, annotations)
        else -> this
    }

val IrType.classifierOrFail: IrClassifierSymbol
    get() = cast<IrSimpleType>().classifier

val IrType.classifierOrNull: IrClassifierSymbol?
    get() = safeAs<IrSimpleType>()?.classifier

fun IrType.makeNotNull() =
    if (this is IrSimpleType && this.hasQuestionMark)
        IrSimpleTypeImpl(
            originalKotlinType,
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
            originalKotlinType,
            classifier,
            true,
            arguments,
            annotations,
            Variance.INVARIANT
        )
    else
        this

fun IrType.toKotlinType(): KotlinType = when (this) {
    is IrSimpleType -> {
        val classifier = this.classifier.descriptor
        val arguments = this.arguments.mapIndexed { index, it ->
            when (it) {
                is IrTypeProjection -> TypeProjectionImpl(it.variance, it.type.toKotlinType())
                is IrStarProjection -> StarProjectionImpl((classifier as ClassDescriptor).declaredTypeParameters[index])
                else -> error(it)
            }
        }

        classifier.defaultType.replace(newArguments = arguments).makeNullableAsSpecified(this.hasQuestionMark)
    }
    else -> TODO(this.toString())
}
