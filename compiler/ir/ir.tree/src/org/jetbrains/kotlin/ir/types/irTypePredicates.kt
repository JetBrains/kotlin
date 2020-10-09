/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.hasEqualFqName
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
    declaration.hasEqualFqName(fqName.toSafe())

fun IrType.isAny(): Boolean = isNotNullClassType(StandardNames.FqNames.any)
fun IrType.isNullableAny(): Boolean = isNullableClassType(StandardNames.FqNames.any)

fun IrType.isString(): Boolean = isNotNullClassType(StandardNames.FqNames.string)
fun IrType.isNullableString(): Boolean = isNullableClassType(StandardNames.FqNames.string)
fun IrType.isStringClassType(): Boolean = isClassType(StandardNames.FqNames.string)
fun IrType.isArray(): Boolean = isNotNullClassType(StandardNames.FqNames.array)
fun IrType.isNullableArray(): Boolean = isNullableClassType(StandardNames.FqNames.array)
fun IrType.isCollection(): Boolean = isNotNullClassType(StandardNames.FqNames.collection.toUnsafe())
fun IrType.isNothing(): Boolean = isNotNullClassType(StandardNames.FqNames.nothing)
fun IrType.isKClass(): Boolean = isNotNullClassType(StandardNames.FqNames.kClass)

fun IrType.isPrimitiveType(): Boolean = StandardNames.FqNames.fqNameToPrimitiveType.keys.any { isNotNullClassType(it) }
fun IrType.isNullablePrimitiveType(): Boolean = StandardNames.FqNames.fqNameToPrimitiveType.keys.any { isNullableClassType(it) }

fun IrType.isMarkedNullable() = (this as? IrSimpleType)?.hasQuestionMark ?: false

fun IrType.isUnit() = isNotNullClassType(StandardNames.FqNames.unit)

fun IrType.isBoolean(): Boolean = isNotNullClassType(StandardNames.FqNames._boolean)
fun IrType.isChar(): Boolean = isNotNullClassType(StandardNames.FqNames._char)
fun IrType.isByte(): Boolean = isNotNullClassType(StandardNames.FqNames._byte)
fun IrType.isShort(): Boolean = isNotNullClassType(StandardNames.FqNames._short)
fun IrType.isInt(): Boolean = isNotNullClassType(StandardNames.FqNames._int)
fun IrType.isLong(): Boolean = isNotNullClassType(StandardNames.FqNames._long)
fun IrType.isUByte(): Boolean = isNotNullClassType(StandardNames.FqNames.uByteFqName.toUnsafe())
fun IrType.isUShort(): Boolean = isNotNullClassType(StandardNames.FqNames.uShortFqName.toUnsafe())
fun IrType.isUInt(): Boolean = isNotNullClassType(StandardNames.FqNames.uIntFqName.toUnsafe())
fun IrType.isULong(): Boolean = isNotNullClassType(StandardNames.FqNames.uLongFqName.toUnsafe())
fun IrType.isFloat(): Boolean = isNotNullClassType(StandardNames.FqNames._float)
fun IrType.isDouble(): Boolean = isNotNullClassType(StandardNames.FqNames._double)
fun IrType.isNumber(): Boolean = isNotNullClassType(StandardNames.FqNames.number)

fun IrType.isComparable(): Boolean = isNotNullClassType(StandardNames.FqNames.comparable.toUnsafe())
fun IrType.isCharSequence(): Boolean = isNotNullClassType(StandardNames.FqNames.charSequence)
fun IrType.isIterable(): Boolean = isNotNullClassType(StandardNames.FqNames.iterable.toUnsafe())
fun IrType.isSequence(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.sequences.Sequence"))

fun IrType.isBooleanArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.BooleanArray"))
fun IrType.isCharArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.CharArray"))
fun IrType.isByteArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.ByteArray"))
fun IrType.isShortArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.ShortArray"))
fun IrType.isIntArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.IntArray"))
fun IrType.isLongArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.LongArray"))
fun IrType.isFloatArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.FloatArray"))
fun IrType.isDoubleArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.DoubleArray"))

fun IrType.isKotlinResult(): Boolean = isNotNullClassType(StandardNames.RESULT_FQ_NAME.toUnsafe())
fun IrType.isNullableContinuation(): Boolean = isNullableClassType(StandardNames.CONTINUATION_INTERFACE_FQ_NAME_RELEASE.toUnsafe())
