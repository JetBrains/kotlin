/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker

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

fun TypeSystemCommonBackendContext.computeExpandedTypeForInlineClass(inlineClassType: KotlinTypeMarker): KotlinTypeMarker? =
    computeExpandedTypeInner(inlineClassType, hashSetOf())

private fun TypeSystemCommonBackendContext.computeExpandedTypeInner(
    kotlinType: KotlinTypeMarker, visitedClassifiers: HashSet<TypeConstructorMarker>
): KotlinTypeMarker? {
    val classifier = kotlinType.typeConstructor()
    if (!visitedClassifiers.add(classifier)) return null

    val typeParameter = classifier.getTypeParameterClassifier()

    return when {
        typeParameter != null -> {
            val upperBound = typeParameter.getRepresentativeUpperBound()
            computeExpandedTypeInner(upperBound, visitedClassifiers)
                ?.let { expandedUpperBound ->
                    val upperBoundIsPrimitiveOrInlineClass =
                        upperBound.typeConstructor().isInlineClass() || upperBound is SimpleTypeMarker && upperBound.isPrimitiveType()
                    when {
                        expandedUpperBound is SimpleTypeMarker && expandedUpperBound.isPrimitiveType() &&
                                kotlinType.isNullableType() && upperBoundIsPrimitiveOrInlineClass -> upperBound.makeNullable()
                        expandedUpperBound.isNullableType() || !kotlinType.isMarkedNullable() -> expandedUpperBound
                        else -> expandedUpperBound.makeNullable()
                    }
                }
        }

        classifier.isInlineClass() -> {
            // kotlinType is the boxed inline class type

            val underlyingType = kotlinType.typeConstructor().getUnsubstitutedUnderlyingType() ?: return null
            val expandedUnderlyingType = computeExpandedTypeInner(underlyingType, visitedClassifiers) ?: return null
            when {
                !kotlinType.isNullableType() -> expandedUnderlyingType

                // Here inline class type is nullable. Apply nullability to the expandedUnderlyingType.

                // Nullable types become inline class boxes
                expandedUnderlyingType.isNullableType() -> kotlinType

                // Primitives become inline class boxes
                expandedUnderlyingType is SimpleTypeMarker && expandedUnderlyingType.isPrimitiveType() -> kotlinType

                // Non-null reference types become nullable reference types
                else -> expandedUnderlyingType.makeNullable()
            }
        }

        else -> kotlinType
    }
}
