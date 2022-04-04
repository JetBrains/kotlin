/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.lazy.types

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.types.ErasureTypeAttributes
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeUsage

data class JavaTypeAttributes(
    override val howThisTypeIsUsed: TypeUsage,
    val flexibility: JavaTypeFlexibility = JavaTypeFlexibility.INFLEXIBLE,
    val isRaw: Boolean = false,
    val isForAnnotationParameter: Boolean = false,
    // we use it to prevent happening a recursion while compute type parameter's upper bounds
    override val visitedTypeParameters: Set<TypeParameterDescriptor>? = null,
    override val defaultType: SimpleType? = null
) : ErasureTypeAttributes(howThisTypeIsUsed, visitedTypeParameters, defaultType) {
    fun withFlexibility(flexibility: JavaTypeFlexibility) = copy(flexibility = flexibility)
    fun markIsRaw(isRaw: Boolean) = copy(isRaw = isRaw)
    override fun withDefaultType(type: SimpleType?) = copy(defaultType = type)
    override fun withNewVisitedTypeParameter(typeParameter: TypeParameterDescriptor) =
        copy(visitedTypeParameters = if (visitedTypeParameters != null) visitedTypeParameters + typeParameter else setOf(typeParameter))

    override fun equals(other: Any?): Boolean {
        if (other !is JavaTypeAttributes) return false
        return other.defaultType == this.defaultType
                && other.howThisTypeIsUsed == this.howThisTypeIsUsed
                && other.flexibility == this.flexibility
                && other.isRaw == this.isRaw
                && other.isForAnnotationParameter == this.isForAnnotationParameter
    }

    override fun hashCode(): Int {
        var result = defaultType.hashCode()
        result += 31 * result + howThisTypeIsUsed.hashCode()
        result += 31 * result + flexibility.hashCode()
        result += 31 * result + if (isRaw) 1 else 0
        result += 31 * result + if (isForAnnotationParameter) 1 else 0
        return result
    }
}

fun TypeUsage.toAttributes(
    isForAnnotationParameter: Boolean = false,
    isRaw: Boolean = false,
    upperBoundForTypeParameter: TypeParameterDescriptor? = null
) = JavaTypeAttributes(
    this,
    isRaw = isRaw,
    isForAnnotationParameter = isForAnnotationParameter,
    visitedTypeParameters = upperBoundForTypeParameter?.let(::setOf)
)
