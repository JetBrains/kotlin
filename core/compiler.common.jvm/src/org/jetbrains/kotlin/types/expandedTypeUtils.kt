/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

private fun TypeSystemCommonBackendContext.isPrimitiveOrBoundedByPrimitive(type: KotlinTypeMarker): Boolean =
    type.typeConstructor().getTypeParameterClassifier()
        ?.let { !type.isMarkedNullable() && isPrimitiveOrBoundedByPrimitive(it.getRepresentativeUpperBound()) }
        ?: (type is SimpleTypeMarker && type.isPrimitiveType())

fun TypeSystemCommonBackendContext.unwrapTypeParameters(type: KotlinTypeMarker): KotlinTypeMarker =
    type.typeConstructor().getTypeParameterClassifier()?.getRepresentativeUpperBound()?.let {
        if (type.isNullableType()) it.makeNullable() else it
    } ?: type

fun TypeSystemCommonBackendContext.inlineClassUnboxedType(type: KotlinTypeMarker): KotlinTypeMarker? {
    val inlineClassType = unwrapTypeParameters(type)
    val constructor = inlineClassType.typeConstructor()
    val underlying = constructor.getUnsubstitutedUnderlyingType() ?: return null
    // inline class A<T>(val x: T)            underlying = T, unsubstituted = T
    // inline class B<V>(val x: A<V>)         underlying = A<V>, unsubstituted = V
    // inline class C<T : B<U>?, U>(val x: T) underlying = T, unsubstituted = U?
    val unsubstituted = inlineClassUnboxedType(underlying) ?: underlying

    val isNullable = inlineClassType.isNullableType()
    if (isNullable && (unsubstituted.isNullableType() || isPrimitiveOrBoundedByPrimitive(unsubstituted)))
        return null // don't swap one box for another

    val arguments = inlineClassType.getArguments().mapIndexed { i, arg ->
        if (arg.isStarProjection())
            constructor.getParameter(i).getRepresentativeUpperBound().asTypeArgument()
        else arg
    }
    val substituted = unsubstituted.substitute(constructor, arguments)
    return if (isNullable) substituted.makeNullable() else substituted
}
