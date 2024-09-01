/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds

val ConeKotlinType.isByte: Boolean get() = isBuiltinType(StandardClassIds.Byte, isNullable = false)
val ConeKotlinType.isShort: Boolean get() = isBuiltinType(StandardClassIds.Short, isNullable = false)
val ConeKotlinType.isInt: Boolean get() = isBuiltinType(StandardClassIds.Int, isNullable = false)
val ConeKotlinType.isLong: Boolean get() = isBuiltinType(StandardClassIds.Long, isNullable = false)
val ConeKotlinType.isFloat: Boolean get() = isBuiltinType(StandardClassIds.Float, isNullable = false)
val ConeKotlinType.isDouble: Boolean get() = isBuiltinType(StandardClassIds.Double, isNullable = false)

val ConeKotlinType.isAny: Boolean get() = isBuiltinType(StandardClassIds.Any, isNullable = false)
val ConeKotlinType.isNullableAny: Boolean get() = isBuiltinType(StandardClassIds.Any, isNullable = true)
val ConeKotlinType.isAnyOrNullableAny: Boolean get() = isBuiltinType(StandardClassIds.Any, isNullable = null)
val ConeKotlinType.isNothing: Boolean get() = isBuiltinType(StandardClassIds.Nothing, isNullable = false)
val ConeKotlinType.isNullableNothing: Boolean get() = isBuiltinType(StandardClassIds.Nothing, isNullable = true)
val ConeKotlinType.isNothingOrNullableNothing: Boolean get() = isBuiltinType(StandardClassIds.Nothing, isNullable = null)

val ConeKotlinType.isUnit: Boolean get() = isBuiltinType(StandardClassIds.Unit, isNullable = false)
val ConeKotlinType.isUnitOrNullableUnit: Boolean get() = isBuiltinType(StandardClassIds.Unit, isNullable = null)
val ConeKotlinType.isBoolean: Boolean get() = isBuiltinType(StandardClassIds.Boolean, isNullable = false)
val ConeKotlinType.isNullableBoolean: Boolean get() = isBuiltinType(StandardClassIds.Boolean, isNullable = true)
val ConeKotlinType.isBooleanOrNullableBoolean: Boolean get() = isBuiltinType(StandardClassIds.Boolean, isNullable = null)

val ConeKotlinType.isThrowableOrNullableThrowable: Boolean get() = isAnyOfBuiltinType(setOf(StandardClassIds.Throwable))

val ConeKotlinType.isChar: Boolean get() = isBuiltinType(StandardClassIds.Char, isNullable = false)
val ConeKotlinType.isCharOrNullableChar: Boolean get() = isAnyOfBuiltinType(setOf(StandardClassIds.Char))
val ConeKotlinType.isString: Boolean get() = isBuiltinType(StandardClassIds.String, isNullable = false)
val ConeKotlinType.isNullableString: Boolean get() = isBuiltinType(StandardClassIds.String, isNullable = true)

val ConeKotlinType.isEnum: Boolean get() = isBuiltinType(StandardClassIds.Enum, isNullable = false)

val ConeKotlinType.isList: Boolean get() = isBuiltinType(StandardClassIds.List, isNullable = false)
val ConeKotlinType.isMutableList: Boolean get() = isBuiltinType(StandardClassIds.MutableList, isNullable = false)
val ConeKotlinType.isSet: Boolean get() = isBuiltinType(StandardClassIds.Set, isNullable = false)
val ConeKotlinType.isMutableSet: Boolean get() = isBuiltinType(StandardClassIds.MutableSet, isNullable = false)
val ConeKotlinType.isMap: Boolean get() = isBuiltinType(StandardClassIds.Map, isNullable = false)
val ConeKotlinType.isMutableMap: Boolean get() = isBuiltinType(StandardClassIds.MutableMap, isNullable = false)

val ConeKotlinType.isUByte: Boolean get() = isBuiltinType(StandardClassIds.UByte, isNullable = false)
val ConeKotlinType.isUShort: Boolean get() = isBuiltinType(StandardClassIds.UShort, isNullable = false)
val ConeKotlinType.isUInt: Boolean get() = isBuiltinType(StandardClassIds.UInt, isNullable = false)
val ConeKotlinType.isULong: Boolean get() = isBuiltinType(StandardClassIds.ULong, isNullable = false)
val ConeKotlinType.isPrimitiveOrNullablePrimitive: Boolean get() = isAnyOfBuiltinType(StandardClassIds.primitiveTypes)
val ConeKotlinType.isPrimitive: Boolean get() = isPrimitiveOrNullablePrimitive && nullability == ConeNullability.NOT_NULL
val ConeKotlinType.isPrimitiveNumberOrNullableType: Boolean
    get() = isPrimitiveOrNullablePrimitive && !isBooleanOrNullableBoolean && !isCharOrNullableChar

val ConeKotlinType.isArrayType: Boolean get() = isArrayType(isNullable = false)
val ConeKotlinType.isArrayTypeOrNullableArrayType: Boolean get() = isArrayType(isNullable = null)

// Same as [KotlinBuiltIns#isNonPrimitiveArray]
val ConeKotlinType.isNonPrimitiveArray: Boolean
    get() = classLikeLookupTagIfAny?.classId == StandardClassIds.Array

val ConeKotlinType.isPrimitiveArray: Boolean
    get() = classLikeLookupTagIfAny?.classId in StandardClassIds.primitiveArrayTypeByElementType.values

val ConeKotlinType.isUnsignedArray: Boolean
    get() = classLikeLookupTagIfAny?.classId in StandardClassIds.unsignedArrayTypeByElementType.values

val ConeKotlinType.isPrimitiveOrUnsignedArray: Boolean
    get() = isPrimitiveArray || isUnsignedArray

val ConeKotlinType.isUnsignedTypeOrNullableUnsignedType: Boolean get() = isAnyOfBuiltinType(StandardClassIds.unsignedTypes)
val ConeKotlinType.isUnsignedType: Boolean get() = isUnsignedTypeOrNullableUnsignedType && nullability == ConeNullability.NOT_NULL

private fun ConeKotlinType.isBuiltinType(classId: ClassId, isNullable: Boolean?): Boolean {
    if (this !is ConeClassLikeType) return false
    return lookupTag.classId == classId && (isNullable == null || type.isNullable == isNullable)
}

private fun ConeKotlinType.isAnyOfBuiltinType(classIds: Set<ClassId>): Boolean {
    if (this !is ConeClassLikeType) return false
    return lookupTag.classId in classIds
}

private fun ConeKotlinType.isArrayType(isNullable: Boolean?): Boolean {
    return isBuiltinType(StandardClassIds.Array, isNullable) ||
            StandardClassIds.primitiveArrayTypeByElementType.values.any { isBuiltinType(it, isNullable) } ||
            StandardClassIds.unsignedArrayTypeByElementType.values.any { isBuiltinType(it, isNullable) }
}
