/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrScript
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection

/**
 * Perform as much type erasure as is significant for JVM signature generation.
 * Class types are kept as is, while type parameters are replaced with their
 * erased upper bounds, keeping the nullability information.
 *
 * For example, a type parameter `T?` where `T : Any`, `T : Comparable<T>` is
 * erased to `Any?`.
 *
 * Type arguments to the erased upper bound are replaced by `*`, since
 * recursive erasure could loop. For example, a type parameter
 * `T : Comparable<T>` is replaced by `Comparable<*>`.
 */
fun IrType.eraseTypeParameters(): IrType = when (this) {
    is IrSimpleType ->
        when (val owner = classifier.owner) {
            is IrScript -> {
                assert(arguments.isEmpty()) { "Script can't be generic: " + owner.render() }
                IrSimpleTypeImpl(classifier, nullability, emptyList(), annotations)
            }
            is IrClass -> IrSimpleTypeImpl(classifier, nullability, arguments.map { it.eraseTypeParameters() }, annotations)
            is IrTypeParameter -> owner.erasedType(isNullable())
            else -> error("Unknown IrSimpleType classifier kind: $owner")
        }
    is IrErrorType ->
        this
    else -> error("Unknown IrType kind: $this")
}

fun IrType.eraseIfTypeParameter(): IrType {
    val typeParameter = (this as? IrSimpleType)?.classifier?.owner as? IrTypeParameter ?: return this
    return typeParameter.erasedType(isNullable())
}

private fun IrTypeParameter.erasedType(isNullable: Boolean): IrType {
    val upperBound = erasedUpperBound
    return IrSimpleTypeImpl(
        upperBound.symbol,
        isNullable,
        // Should not affect JVM signature, but may result in an invalid type object
        List(upperBound.typeParameters.size) { IrStarProjectionImpl },
        annotations
    )
}

private fun IrTypeArgument.eraseTypeParameters(): IrTypeArgument = when (this) {
    is IrStarProjection -> this
    is IrTypeProjection -> makeTypeProjection(type.eraseTypeParameters(), variance)
}

/**
 * Computes the erased class for this type parameter according to the java erasure rules.
 */
val IrTypeParameter.erasedUpperBound: IrClass
    get() {
        // Pick the (necessarily unique) non-interface upper bound if it exists
        for (type in superTypes) {
            val irClass = type.classOrNull?.owner ?: continue
            if (!irClass.isInterface && !irClass.isAnnotationClass) return irClass
        }

        // Otherwise, choose either the first IrClass supertype or recurse.
        // In the first case, all supertypes are interface types and the choice was arbitrary.
        // In the second case, there is only a single supertype.
        return superTypes.first().erasedUpperBound
    }

val IrType.erasedUpperBound: IrClass
    get() = when (this) {
        is IrSimpleType -> when (val classifier = classifier.owner) {
            is IrClass -> classifier
            is IrTypeParameter -> classifier.erasedUpperBound
            is IrScript -> classifier.targetClass?.owner ?: error(render())
            else -> error(render())
        }
        is IrErrorType -> symbol.owner
        else -> error(render())
    }