/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds

val ConeKotlinType.isByte: Boolean get() = isBuiltinType(StandardClassIds.Byte, false)
val ConeKotlinType.isShort: Boolean get() = isBuiltinType(StandardClassIds.Short, false)
val ConeKotlinType.isInt: Boolean get() = isBuiltinType(StandardClassIds.Int, false)
val ConeKotlinType.isLong: Boolean get() = isBuiltinType(StandardClassIds.Long, false)
val ConeKotlinType.isFloat: Boolean get() = isBuiltinType(StandardClassIds.Float, false)
val ConeKotlinType.isDouble: Boolean get() = isBuiltinType(StandardClassIds.Double, false)

val ConeKotlinType.isAny: Boolean get() = isBuiltinType(StandardClassIds.Any, false)
val ConeKotlinType.isNullableAny: Boolean get() = isBuiltinType(StandardClassIds.Any, true)
val ConeKotlinType.isNothing: Boolean get() = isBuiltinType(StandardClassIds.Nothing, false)
val ConeKotlinType.isNullableNothing: Boolean get() = isBuiltinType(StandardClassIds.Nothing, true)
val ConeKotlinType.isNothingOrNullableNothing: Boolean get() = isBuiltinType(StandardClassIds.Nothing, null)

val ConeKotlinType.isUnit: Boolean get() = isBuiltinType(StandardClassIds.Unit, false)
val ConeKotlinType.isBoolean: Boolean get() = isBuiltinType(StandardClassIds.Boolean, false)
val ConeKotlinType.isNullableBoolean: Boolean get() = isBuiltinType(StandardClassIds.Boolean, true)
val ConeKotlinType.isBooleanOrNullableBoolean: Boolean get() = isBuiltinType(StandardClassIds.Boolean, null)

val ConeKotlinType.isThrowableOrNullableThrowable: Boolean get() = isAnyOfBuiltinType(setOf(StandardClassIds.Throwable))

val ConeKotlinType.isChar: Boolean get() = isBuiltinType(StandardClassIds.Char, false)
val ConeKotlinType.isCharOrNullableChar: Boolean get() = isAnyOfBuiltinType(setOf(StandardClassIds.Char))
val ConeKotlinType.isString: Boolean get() = isBuiltinType(StandardClassIds.String, false)
val ConeKotlinType.isNullableString: Boolean get() = isBuiltinType(StandardClassIds.String, true)

val ConeKotlinType.isEnum: Boolean get() = isBuiltinType(StandardClassIds.Enum, false)

val ConeKotlinType.isList: Boolean get() = isBuiltinType(StandardClassIds.List, false)
val ConeKotlinType.isMutableList: Boolean get() = isBuiltinType(StandardClassIds.MutableList, false)
val ConeKotlinType.isSet: Boolean get() = isBuiltinType(StandardClassIds.Set, false)
val ConeKotlinType.isMutableSet: Boolean get() = isBuiltinType(StandardClassIds.MutableSet, false)
val ConeKotlinType.isMap: Boolean get() = isBuiltinType(StandardClassIds.Map, false)
val ConeKotlinType.isMutableMap: Boolean get() = isBuiltinType(StandardClassIds.MutableMap, false)

val ConeKotlinType.isUByte: Boolean get() = isBuiltinType(StandardClassIds.UByte, false)
val ConeKotlinType.isUShort: Boolean get() = isBuiltinType(StandardClassIds.UShort, false)
val ConeKotlinType.isUInt: Boolean get() = isBuiltinType(StandardClassIds.UInt, false)
val ConeKotlinType.isULong: Boolean get() = isBuiltinType(StandardClassIds.ULong, false)
val ConeKotlinType.isPrimitiveOrNullablePrimitive: Boolean get() = isAnyOfBuiltinType(StandardClassIds.primitiveTypes)
val ConeKotlinType.isPrimitive: Boolean get() = isPrimitiveOrNullablePrimitive && nullability == ConeNullability.NOT_NULL
val ConeKotlinType.isPrimitiveNumberOrNullableType: Boolean
    get() = isPrimitiveOrNullablePrimitive && !isBooleanOrNullableBoolean && !isCharOrNullableChar

val ConeKotlinType.isArrayType: Boolean get() = isArrayType(isNullable = false)
val ConeKotlinType.isArrayTypeOrNullableArrayType: Boolean get() = isArrayType(isNullable = null)

// Same as [KotlinBuiltIns#isNonPrimitiveArray]
val ConeKotlinType.isNonPrimitiveArray: Boolean
    get() = this is ConeClassLikeType && lookupTag.classId == StandardClassIds.Array

val ConeKotlinType.isPrimitiveArray: Boolean
    get() = this is ConeClassLikeType && lookupTag.classId in StandardClassIds.primitiveArrayTypeByElementType.values

val ConeKotlinType.isUnsignedArray: Boolean
    get() = this is ConeClassLikeType && lookupTag.classId in StandardClassIds.unsignedArrayTypeByElementType.values

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