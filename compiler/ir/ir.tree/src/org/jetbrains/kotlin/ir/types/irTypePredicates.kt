/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.builtins.KotlinBuiltInsNames
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.name.FqNameUnsafe

private fun IrType.isNotNullClassType(fqName: FqNameUnsafe) = isClassType(fqName, hasQuestionMark = false)
private fun IrType.isNullableClassType(fqName: FqNameUnsafe) = isClassType(fqName, hasQuestionMark = true)

private fun IrType.isClassType(fqName: FqNameUnsafe, hasQuestionMark: Boolean? = null): Boolean {
    if (this !is IrSimpleType) return false
    if (hasQuestionMark != null && this.hasQuestionMark != hasQuestionMark) return false
    return classifier.isClassWithFqName(fqName)
}

fun IrClassifierSymbol.isClassWithFqName(fqName: FqNameUnsafe): Boolean =
    this is IrClassSymbol && classFqNameEquals(this, fqName)

private fun classFqNameEquals(symbol: IrClassSymbol, fqName: FqNameUnsafe): Boolean {
    assert(symbol.isBound)
    return classFqNameEquals(symbol.owner, fqName)
}

private fun classFqNameEquals(declaration: IrClass, fqName: FqNameUnsafe): Boolean =
    declaration.name == fqName.shortName() && fqName == declaration.fqNameWhenAvailable?.toUnsafe()

fun IrType.isAny(): Boolean = isNotNullClassType(KotlinBuiltInsNames.FqNames.any)
fun IrType.isNullableAny(): Boolean = isNullableClassType(KotlinBuiltInsNames.FqNames.any)

fun IrType.isString(): Boolean = isNotNullClassType(KotlinBuiltInsNames.FqNames.string)
fun IrType.isNullableString(): Boolean = isNullableClassType(KotlinBuiltInsNames.FqNames.string)
fun IrType.isStringClassType(): Boolean = isClassType(KotlinBuiltInsNames.FqNames.string)
fun IrType.isArray(): Boolean = isNotNullClassType(KotlinBuiltInsNames.FqNames.array)
fun IrType.isNullableArray(): Boolean = isNullableClassType(KotlinBuiltInsNames.FqNames.array)
fun IrType.isCollection(): Boolean = isNotNullClassType(KotlinBuiltInsNames.FqNames.collection.toUnsafe())
fun IrType.isNothing(): Boolean = isNotNullClassType(KotlinBuiltInsNames.FqNames.nothing)
fun IrType.isKClass(): Boolean = isNotNullClassType(KotlinBuiltInsNames.FqNames.kClass)

fun IrType.isPrimitiveType(): Boolean = KotlinBuiltInsNames.FqNames.fqNameToPrimitiveType.keys.any { isNotNullClassType(it) }
fun IrType.isNullablePrimitiveType(): Boolean = KotlinBuiltInsNames.FqNames.fqNameToPrimitiveType.keys.any { isNullableClassType(it) }

fun IrType.isMarkedNullable() = (this as? IrSimpleType)?.hasQuestionMark ?: false

fun IrType.isUnit() = isNotNullClassType(KotlinBuiltInsNames.FqNames.unit)

fun IrType.isBoolean(): Boolean = isNotNullClassType(KotlinBuiltInsNames.FqNames._boolean)
fun IrType.isChar(): Boolean = isNotNullClassType(KotlinBuiltInsNames.FqNames._char)
fun IrType.isByte(): Boolean = isNotNullClassType(KotlinBuiltInsNames.FqNames._byte)
fun IrType.isShort(): Boolean = isNotNullClassType(KotlinBuiltInsNames.FqNames._short)
fun IrType.isInt(): Boolean = isNotNullClassType(KotlinBuiltInsNames.FqNames._int)
fun IrType.isLong(): Boolean = isNotNullClassType(KotlinBuiltInsNames.FqNames._long)
fun IrType.isUByte(): Boolean = isNotNullClassType(KotlinBuiltInsNames.FqNames.uByteFqName.toUnsafe())
fun IrType.isUShort(): Boolean = isNotNullClassType(KotlinBuiltInsNames.FqNames.uShortFqName.toUnsafe())
fun IrType.isUInt(): Boolean = isNotNullClassType(KotlinBuiltInsNames.FqNames.uIntFqName.toUnsafe())
fun IrType.isULong(): Boolean = isNotNullClassType(KotlinBuiltInsNames.FqNames.uLongFqName.toUnsafe())
fun IrType.isFloat(): Boolean = isNotNullClassType(KotlinBuiltInsNames.FqNames._float)
fun IrType.isDouble(): Boolean = isNotNullClassType(KotlinBuiltInsNames.FqNames._double)
fun IrType.isNumber(): Boolean = isNotNullClassType(KotlinBuiltInsNames.FqNames.number)

fun IrType.isComparable(): Boolean = isNotNullClassType(KotlinBuiltInsNames.FqNames.comparable.toUnsafe())
fun IrType.isCharSequence(): Boolean = isNotNullClassType(KotlinBuiltInsNames.FqNames.charSequence)
fun IrType.isIterable(): Boolean = isNotNullClassType(KotlinBuiltInsNames.FqNames.iterable.toUnsafe())
fun IrType.isSequence(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.sequences.Sequence"))

fun IrType.isBooleanArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.BooleanArray"))
fun IrType.isCharArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.CharArray"))
fun IrType.isByteArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.ByteArray"))
fun IrType.isShortArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.ShortArray"))
fun IrType.isIntArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.IntArray"))
fun IrType.isLongArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.LongArray"))
fun IrType.isFloatArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.FloatArray"))
fun IrType.isDoubleArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.DoubleArray"))

fun IrType.isKotlinResult(): Boolean = isNotNullClassType(KotlinBuiltInsNames.RESULT_FQ_NAME.toUnsafe())
fun IrType.isNullableContinuation(): Boolean = isNullableClassType(KotlinBuiltInsNames.CONTINUATION_INTERFACE_FQ_NAME_RELEASE.toUnsafe())
