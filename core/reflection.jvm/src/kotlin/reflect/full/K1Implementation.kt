/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.full

import org.jetbrains.kotlin.types.*
import kotlin.reflect.KClassifier
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.jvm.internal.KClassImpl
import kotlin.reflect.jvm.internal.KTypeParameterImpl
import kotlin.reflect.jvm.internal.KTypeParameterOwnerImpl
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError
import kotlin.reflect.jvm.internal.types.DescriptorKType

internal fun KClassifier.createK1KType(
    arguments: List<KTypeProjection>,
    container: KTypeParameterOwnerImpl?,
    nullable: Boolean,
): DescriptorKType {
    val descriptor = when (this) {
        is KClassImpl<*> -> descriptor
        is KTypeParameterImpl -> descriptor
        else -> throw KotlinReflectionInternalError("Cannot create type for an unsupported classifier: $this (${this.javaClass})")
    }

    checkArgumentsSize(descriptor.typeConstructor.parameters.size, arguments.size)

    return DescriptorKType(container, createKotlinType(descriptor.typeConstructor, arguments, nullable))
}

private fun createKotlinType(typeConstructor: TypeConstructor, arguments: List<KTypeProjection>, nullable: Boolean): SimpleType {
    val parameters = typeConstructor.parameters
    return KotlinTypeFactory.simpleType(TypeAttributes.Empty, typeConstructor, arguments.mapIndexed { index, typeProjection ->
        val type = (typeProjection.type as DescriptorKType?)?.type
        when (typeProjection.variance) {
            KVariance.INVARIANT -> TypeProjectionImpl(Variance.INVARIANT, type!!)
            KVariance.IN -> TypeProjectionImpl(Variance.IN_VARIANCE, type!!)
            KVariance.OUT -> TypeProjectionImpl(Variance.OUT_VARIANCE, type!!)
            null -> StarProjectionImpl(parameters[index])
        }
    }, nullable)
}
