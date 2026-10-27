/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds

object StandardTypes {
    val Boolean: ConeClassLikeType = StandardClassIds.Boolean.constructClassLikeType()
    val Char: ConeClassLikeType = StandardClassIds.Char.constructClassLikeType()
    val Byte: ConeClassLikeType = StandardClassIds.Byte.constructClassLikeType()
    val Short: ConeClassLikeType = StandardClassIds.Short.constructClassLikeType()
    val Int: ConeClassLikeType = StandardClassIds.Int.constructClassLikeType()
    val Long: ConeClassLikeType = StandardClassIds.Long.constructClassLikeType()
    val Float: ConeClassLikeType = StandardClassIds.Float.constructClassLikeType()
    val Double: ConeClassLikeType = StandardClassIds.Double.constructClassLikeType()

    val Any: ConeClassLikeType = StandardClassIds.Any.constructClassLikeType()
    val NullableAny: ConeClassLikeType = StandardClassIds.Any.constructClassLikeType(isMarkedNullable = true)
    val Nothing: ConeClassLikeType = StandardClassIds.Nothing.constructClassLikeType()
    val NullableNothing: ConeClassLikeType = StandardClassIds.Nothing.constructClassLikeType(isMarkedNullable = true)
    val Unit: ConeClassLikeType = StandardClassIds.Unit.constructClassLikeType()

    val String: ConeClassLikeType = StandardClassIds.String.constructClassLikeType()

    val UByte: ConeClassLikeType = StandardClassIds.UByte.constructClassLikeType()
    val UShort: ConeClassLikeType = StandardClassIds.UShort.constructClassLikeType()
    val UInt: ConeClassLikeType = StandardClassIds.UInt.constructClassLikeType()
    val ULong: ConeClassLikeType = StandardClassIds.ULong.constructClassLikeType()

    val RichError: ConeClassLikeType = StandardClassIds.RichError.constructClassLikeType()
    val NullableRichError: ConeClassLikeType = StandardClassIds.RichError.constructClassLikeType(isMarkedNullable = true)

    fun richError(isMarkedNullable: Boolean): ConeClassLikeType = if (isMarkedNullable) NullableRichError else RichError

    val NullableNonError: ConeClassLikeType = StandardClassIds.NonError.constructClassLikeType(isMarkedNullable = true)
}

fun ConeClassLikeType.isDouble(): Boolean = lookupTag.classId == StandardClassIds.Double
fun ConeClassLikeType.isFloat(): Boolean = lookupTag.classId == StandardClassIds.Float
fun ConeClassLikeType.isLong(): Boolean = lookupTag.classId == StandardClassIds.Long
fun ConeClassLikeType.isInt(): Boolean = lookupTag.classId == StandardClassIds.Int
fun ConeClassLikeType.isShort(): Boolean = lookupTag.classId == StandardClassIds.Short
fun ConeClassLikeType.isByte(): Boolean = lookupTag.classId == StandardClassIds.Byte
fun ConeClassLikeType.isBoolean(): Boolean = lookupTag.classId == StandardClassIds.Boolean
fun ConeClassLikeType.isChar(): Boolean = lookupTag.classId == StandardClassIds.Char

fun ConeClassLikeType.isULong(): Boolean = lookupTag.classId == StandardClassIds.ULong

fun ConeClassLikeType.isPrimitiveType(): Boolean =
    isPrimitiveNumberOrUnsignedNumberType() || isBoolean() || isByte() || isShort() || isChar()

fun ConeClassLikeType.isPrimitiveNumberType(): Boolean = lookupTag.classId in PRIMITIVE_NUMBER_CLASS_IDS
fun ConeClassLikeType.isPrimitiveUnsignedNumberType(): Boolean = lookupTag.classId in PRIMITIVE_UNSIGNED_NUMBER_CLASS_IDS
fun ConeClassLikeType.isPrimitiveNumberOrUnsignedNumberType(): Boolean = isPrimitiveNumberType() || isPrimitiveUnsignedNumberType()

fun FirClass.isDouble(): Boolean = classId == StandardClassIds.Double
fun FirClass.isFloat(): Boolean = classId == StandardClassIds.Float
fun FirClass.isLong(): Boolean = classId == StandardClassIds.Long
fun FirClass.isInt(): Boolean = classId == StandardClassIds.Int
fun FirClass.isShort(): Boolean = classId == StandardClassIds.Short
fun FirClass.isByte(): Boolean = classId == StandardClassIds.Byte
fun FirClass.isBoolean(): Boolean = classId == StandardClassIds.Boolean
fun FirClass.isChar(): Boolean = classId == StandardClassIds.Char

fun FirClass.isPrimitiveType(): Boolean = isPrimitiveNumberOrUnsignedNumberType() || isBoolean() || isByte() || isShort() || isChar()
fun FirClass.isPrimitiveNumberType(): Boolean = classId in PRIMITIVE_NUMBER_CLASS_IDS
fun FirClass.isPrimitiveUnsignedNumberType(): Boolean = classId in PRIMITIVE_UNSIGNED_NUMBER_CLASS_IDS
fun FirClass.isPrimitiveNumberOrUnsignedNumberType(): Boolean = isPrimitiveNumberType() || isPrimitiveUnsignedNumberType()

// --------------------------- symbols ---------------------------

fun FirClassSymbol<*>.isDouble(): Boolean = classId == StandardClassIds.Double
fun FirClassSymbol<*>.isFloat(): Boolean = classId == StandardClassIds.Float
fun FirClassSymbol<*>.isLong(): Boolean = classId == StandardClassIds.Long
fun FirClassSymbol<*>.isInt(): Boolean = classId == StandardClassIds.Int
fun FirClassSymbol<*>.isShort(): Boolean = classId == StandardClassIds.Short
fun FirClassSymbol<*>.isByte(): Boolean = classId == StandardClassIds.Byte
fun FirClassSymbol<*>.isBoolean(): Boolean = classId == StandardClassIds.Boolean
fun FirClassSymbol<*>.isChar(): Boolean = classId == StandardClassIds.Char

fun FirClassSymbol<*>.isPrimitiveType(): Boolean = isPrimitiveNumberOrUnsignedNumberType() || isBoolean() || isByte() || isShort() || isChar()
fun FirClassSymbol<*>.isPrimitiveNumberType(): Boolean = classId in PRIMITIVE_NUMBER_CLASS_IDS
fun FirClassSymbol<*>.isPrimitiveUnsignedNumberType(): Boolean = classId in PRIMITIVE_UNSIGNED_NUMBER_CLASS_IDS
fun FirClassSymbol<*>.isPrimitiveNumberOrUnsignedNumberType(): Boolean = isPrimitiveNumberType() || isPrimitiveUnsignedNumberType()

private val PRIMITIVE_NUMBER_CLASS_IDS: Set<ClassId> = setOf(
    StandardClassIds.Double, StandardClassIds.Float, StandardClassIds.Long, StandardClassIds.Int,
    StandardClassIds.Short, StandardClassIds.Byte
)

private val PRIMITIVE_UNSIGNED_NUMBER_CLASS_IDS: Set<ClassId> = setOf(
    StandardClassIds.ULong, StandardClassIds.UInt, StandardClassIds.UShort, StandardClassIds.UByte
)
