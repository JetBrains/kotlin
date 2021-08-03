/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

val ConeKotlinType.isNullable: Boolean get() = nullability != ConeNullability.NOT_NULL

val ConeKotlinType.isMarkedNullable: Boolean get() = nullability == ConeNullability.NULLABLE

val ConeKotlinType.classId: ClassId? get() = this.safeAs<ConeClassLikeType>()?.lookupTag?.classId

fun ConeKotlinType.contains(predicate: (ConeKotlinType) -> Boolean): Boolean {
    return contains(predicate, null)
}

private fun ConeKotlinType.contains(predicate: (ConeKotlinType) -> Boolean, visited: SmartSet<ConeKotlinType>?): Boolean {
    if (visited?.contains(this) == true) return false
    if (predicate(this)) return true

    @Suppress("NAME_SHADOWING")
    val visited = visited ?: SmartSet.create()
    visited += this

    return when (this) {
        is ConeFlexibleType -> lowerBound.contains(predicate, visited) || upperBound.contains(predicate, visited)
        is ConeDefinitelyNotNullType -> original.contains(predicate, visited)
        is ConeIntersectionType -> intersectedTypes.any { it.contains(predicate, visited) }
        else -> typeArguments.any { it is ConeKotlinTypeProjection && it.type.contains(predicate, visited) }
    }
}

fun ConeClassLikeType.withArguments(typeArguments: Array<out ConeTypeProjection>): ConeClassLikeType = when (this) {
    is ConeClassLikeTypeImpl -> ConeClassLikeTypeImpl(lookupTag, typeArguments, isNullable, attributes)
    is ConeClassErrorType -> this
    else -> error("Unknown cone type: ${this::class}")
}

fun ConeKotlinType.toTypeProjection(variance: Variance): ConeTypeProjection =
    when (variance) {
        Variance.INVARIANT -> this
        Variance.IN_VARIANCE -> ConeKotlinTypeProjectionIn(this)
        Variance.OUT_VARIANCE -> ConeKotlinTypeProjectionOut(this)
    }

fun ConeClassLikeType.replaceArgumentsWithStarProjections(): ConeClassLikeType {
    if (typeArguments.isEmpty()) return this
    val newArguments = Array(typeArguments.size) { ConeStarProjection }
    return withArguments(newArguments)
}

val ConeKotlinType.isAny: Boolean get() = isBuiltinType(StandardClassIds.Any, false)
val ConeKotlinType.isNullableAny: Boolean get() = isBuiltinType(StandardClassIds.Any, true)
val ConeKotlinType.isNothing: Boolean get() = isBuiltinType(StandardClassIds.Nothing, false)
val ConeKotlinType.isNullableNothing: Boolean get() = isBuiltinType(StandardClassIds.Nothing, true)
val ConeKotlinType.isUnit: Boolean get() = isBuiltinType(StandardClassIds.Unit, false)
val ConeKotlinType.isBoolean: Boolean get() = isBuiltinType(StandardClassIds.Boolean, false)
val ConeKotlinType.isNullableBoolean: Boolean get() = isBuiltinType(StandardClassIds.Boolean, true)
val ConeKotlinType.isBooleanOrNullableBoolean: Boolean get() = isAnyOfBuiltinType(setOf(StandardClassIds.Boolean))
val ConeKotlinType.isEnum: Boolean get() = isBuiltinType(StandardClassIds.Enum, false)
val ConeKotlinType.isString: Boolean get() = isBuiltinType(StandardClassIds.String, false)
val ConeKotlinType.isPrimitiveOrNullablePrimitive: Boolean get() = isAnyOfBuiltinType(StandardClassIds.primitiveTypes)
val ConeKotlinType.isPrimitive: Boolean get() = isPrimitiveOrNullablePrimitive && nullability == ConeNullability.NOT_NULL
val ConeKotlinType.isArrayType: Boolean
    get() {
        return isBuiltinType(StandardClassIds.Array, false) ||
                StandardClassIds.primitiveArrayTypeByElementType.values.any { isBuiltinType(it, false) }
    }
// Same as [KotlinBuiltIns#isNonPrimitiveArray]
val ConeKotlinType.isNonPrimitiveArray: Boolean
    get() = this is ConeClassLikeType && lookupTag.classId == StandardClassIds.Array

val ConeKotlinType.isPrimitiveArray: Boolean
    get() = this is ConeClassLikeType && lookupTag.classId in StandardClassIds.primitiveArrayTypeByElementType.values

val ConeKotlinType.isUnsignedTypeOrNullableUnsignedType: Boolean get() = isAnyOfBuiltinType(StandardClassIds.unsignedTypes)
val ConeKotlinType.isUnsignedType: Boolean get() = isUnsignedTypeOrNullableUnsignedType && nullability == ConeNullability.NOT_NULL

private val builtinIntegerTypes = setOf(StandardClassIds.Int, StandardClassIds.Byte, StandardClassIds.Long, StandardClassIds.Short)
val ConeKotlinType.isIntegerTypeOrNullableIntegerTypeOfAnySize: Boolean get() = isAnyOfBuiltinType(builtinIntegerTypes)

private fun ConeKotlinType.isBuiltinType(classId: ClassId, isNullable: Boolean?): Boolean {
    if (this !is ConeClassLikeType) return false
    return lookupTag.classId == classId && (isNullable == null || type.isNullable == isNullable)
}

private fun ConeKotlinType.isAnyOfBuiltinType(classIds: Set<ClassId>): Boolean {
    if (this !is ConeClassLikeType) return false
    return lookupTag.classId in classIds
}
