/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.getFqName

private fun IrType.isNotNullClassType(fqName: FqNameUnsafe) = isClassType(fqName, hasQuestionMark = false)
private fun IrType.isNullableClassType(fqName: FqNameUnsafe) = isClassType(fqName, hasQuestionMark = true)

private fun IrType.isClassType(fqName: FqNameUnsafe, hasQuestionMark: Boolean? = null): Boolean {
    if (this !is IrSimpleType) return false
    if (hasQuestionMark != null && this.hasQuestionMark != hasQuestionMark) return false
    return classifier.isClassWithFqName(fqName)
}

fun IrClassifierSymbol.isClassWithFqName(fqName: FqNameUnsafe): Boolean =
    this is IrClassSymbol && classFqNameEquals(this, fqName)

private fun classFqNameEquals(symbol: IrClassSymbol, fqName: FqNameUnsafe): Boolean =
    if (symbol.isBound) classFqNameEquals(symbol.owner, fqName) else classFqNameEquals(symbol.descriptor, fqName)

private fun classFqNameEquals(declaration: IrClass, fqName: FqNameUnsafe): Boolean =
    declaration.name == fqName.shortName() && fqName == declaration.fqNameWhenAvailable?.toUnsafe()

private fun classFqNameEquals(descriptor: ClassDescriptor, fqName: FqNameUnsafe): Boolean =
    descriptor.name == fqName.shortName() && fqName == getFqName(descriptor)

fun IrType.isAny(): Boolean = isNotNullClassType(KotlinBuiltIns.FQ_NAMES.any)
fun IrType.isNullableAny(): Boolean = isNullableClassType(KotlinBuiltIns.FQ_NAMES.any)

fun IrType.isString(): Boolean = isNotNullClassType(KotlinBuiltIns.FQ_NAMES.string)
fun IrType.isNullableString(): Boolean = isNullableClassType(KotlinBuiltIns.FQ_NAMES.string)
fun IrType.isStringClassType(): Boolean = isClassType(KotlinBuiltIns.FQ_NAMES.string)
fun IrType.isArray(): Boolean = isNotNullClassType(KotlinBuiltIns.FQ_NAMES.array)
fun IrType.isNullableArray(): Boolean = isNullableClassType(KotlinBuiltIns.FQ_NAMES.array)
fun IrType.isCollection(): Boolean = isNotNullClassType(KotlinBuiltIns.FQ_NAMES.collection.toUnsafe())
fun IrType.isNothing(): Boolean = isNotNullClassType(KotlinBuiltIns.FQ_NAMES.nothing)
fun IrType.isKClass(): Boolean = isNotNullClassType(KotlinBuiltIns.FQ_NAMES.kClass)

fun IrType.isPrimitiveType(): Boolean = KotlinBuiltIns.FQ_NAMES.fqNameToPrimitiveType.keys.any { isNotNullClassType(it) }
fun IrType.isNullablePrimitiveType(): Boolean = KotlinBuiltIns.FQ_NAMES.fqNameToPrimitiveType.keys.any { isNullableClassType(it) }

fun IrType.isMarkedNullable() = (this as? IrSimpleType)?.hasQuestionMark ?: false

fun IrType.isUnit() = isNotNullClassType(KotlinBuiltIns.FQ_NAMES.unit)
fun IrType.isNullableUnit() = isNullableClassType(KotlinBuiltIns.FQ_NAMES.unit)
fun IrType.isUnitOrNullableUnit() = this.isUnit() || this.isNullableUnit()

fun IrType.isBoolean(): Boolean = isNotNullClassType(KotlinBuiltIns.FQ_NAMES._boolean)
fun IrType.isChar(): Boolean = isNotNullClassType(KotlinBuiltIns.FQ_NAMES._char)
fun IrType.isByte(): Boolean = isNotNullClassType(KotlinBuiltIns.FQ_NAMES._byte)
fun IrType.isShort(): Boolean = isNotNullClassType(KotlinBuiltIns.FQ_NAMES._short)
fun IrType.isInt(): Boolean = isNotNullClassType(KotlinBuiltIns.FQ_NAMES._int)
fun IrType.isLong(): Boolean = isNotNullClassType(KotlinBuiltIns.FQ_NAMES._long)
fun IrType.isUByte(): Boolean = isNotNullClassType(KotlinBuiltIns.FQ_NAMES.uByteFqName.toUnsafe())
fun IrType.isUShort(): Boolean = isNotNullClassType(KotlinBuiltIns.FQ_NAMES.uShortFqName.toUnsafe())
fun IrType.isUInt(): Boolean = isNotNullClassType(KotlinBuiltIns.FQ_NAMES.uIntFqName.toUnsafe())
fun IrType.isULong(): Boolean = isNotNullClassType(KotlinBuiltIns.FQ_NAMES.uLongFqName.toUnsafe())
fun IrType.isFloat(): Boolean = isNotNullClassType(KotlinBuiltIns.FQ_NAMES._float)
fun IrType.isDouble(): Boolean = isNotNullClassType(KotlinBuiltIns.FQ_NAMES._double)
fun IrType.isNumber(): Boolean = isNotNullClassType(KotlinBuiltIns.FQ_NAMES.number)

fun IrType.isComparable(): Boolean = isNotNullClassType(KotlinBuiltIns.FQ_NAMES.comparable.toUnsafe())
fun IrType.isCharSequence(): Boolean = isNotNullClassType(KotlinBuiltIns.FQ_NAMES.charSequence)
fun IrType.isIterable(): Boolean = isNotNullClassType(KotlinBuiltIns.FQ_NAMES.iterable.toUnsafe())
fun IrType.isSequence(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.sequences.Sequence"))

fun IrType.isBooleanArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.BooleanArray"))
fun IrType.isCharArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.CharArray"))
fun IrType.isByteArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.ByteArray"))
fun IrType.isShortArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.ShortArray"))
fun IrType.isIntArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.IntArray"))
fun IrType.isLongArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.LongArray"))
fun IrType.isFloatArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.FloatArray"))
fun IrType.isDoubleArray(): Boolean = isNotNullClassType(FqNameUnsafe("kotlin.DoubleArray"))

fun IrType.isNullableBoolean(): Boolean = isNullableClassType(KotlinBuiltIns.FQ_NAMES._boolean)
fun IrType.isNullableLong(): Boolean = isNullableClassType(KotlinBuiltIns.FQ_NAMES._long)
fun IrType.isNullableChar(): Boolean = isNullableClassType(KotlinBuiltIns.FQ_NAMES._char)

fun IrType.isKotlinResult(): Boolean = isNotNullClassType(DescriptorUtils.RESULT_FQ_NAME.toUnsafe())
fun IrType.isContinuation(): Boolean = isNotNullClassType(DescriptorUtils.CONTINUATION_INTERFACE_FQ_NAME_RELEASE.toUnsafe())
fun IrType.isNullableContinuation(): Boolean = isNullableClassType(DescriptorUtils.CONTINUATION_INTERFACE_FQ_NAME_RELEASE.toUnsafe())
