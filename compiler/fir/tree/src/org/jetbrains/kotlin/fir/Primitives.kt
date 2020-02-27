/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId

object PrimitiveTypes {
    val Boolean = StandardClassIds.Boolean.createType()
    val Char = StandardClassIds.Char.createType()
    val Byte = StandardClassIds.Byte.createType()
    val Short = StandardClassIds.Short.createType()
    val Int = StandardClassIds.Int.createType()
    val Long = StandardClassIds.Long.createType()
    val Float = StandardClassIds.Float.createType()
    val Double = StandardClassIds.Double.createType()
}

private fun ClassId.createType(): ConeClassLikeType = 
    ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(this), emptyArray(), isNullable = false)

fun ConeClassLikeType.isDouble() = lookupTag.classId == StandardClassIds.Double
fun ConeClassLikeType.isFloat() = lookupTag.classId == StandardClassIds.Float
fun ConeClassLikeType.isLong() = lookupTag.classId == StandardClassIds.Long
fun ConeClassLikeType.isInt() = lookupTag.classId == StandardClassIds.Int
fun ConeClassLikeType.isShort() = lookupTag.classId == StandardClassIds.Short
fun ConeClassLikeType.isByte() = lookupTag.classId == StandardClassIds.Byte

private val PRIMITIVE_NUMBER_CLASS_IDS = setOf(
    StandardClassIds.Double, StandardClassIds.Float, StandardClassIds.Long, StandardClassIds.Int,
    StandardClassIds.Short, StandardClassIds.Byte
)

fun ConeClassLikeType.isPrimitiveNumberType() = lookupTag.classId in PRIMITIVE_NUMBER_CLASS_IDS
