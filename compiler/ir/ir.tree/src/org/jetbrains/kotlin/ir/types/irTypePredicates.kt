/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.hasEqualFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.newHashMapWithExpectedSize

@Suppress("ObjectPropertyName")
object IdSignatureValues {
    @JvmField val any = getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, "Any")
    @JvmField val nothing = getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME,"Nothing")
    @JvmField val unit = getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME,"Unit")
    @JvmField val _boolean = getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, "Boolean")
    @JvmField val _char = getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, "Char")
    @JvmField val _byte = getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME,"Byte")
    @JvmField val _short = getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME,"Short")
    @JvmField val _int = getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME,"Int")
    @JvmField val _long = getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME,"Long")
    @JvmField val _float = getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME,"Float")
    @JvmField val _double = getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME,"Double")
    @JvmField val number = getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME,"Number")
    @JvmField val uByte = getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME,"UByte")
    @JvmField val uShort = getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME,"UShort")
    @JvmField val uInt = getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME,"UInt")
    @JvmField val uLong = getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME,"ULong")
    @JvmField val string = getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME,"String")
    @JvmField val array = getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME,"Array")
    @JvmField val collection = getPublicSignature(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, "Collection")
    @JvmField val kClass = getPublicSignature(StandardNames.KOTLIN_REFLECT_FQ_NAME, "KClass")
    @JvmField val comparable = getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME,"Comparable")
    @JvmField val charSequence = getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME,"CharSequence")
    @JvmField val iterable = getPublicSignature(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, "Iterable")
    @JvmField val continuation = getPublicSignature(StandardNames.COROUTINES_PACKAGE_FQ_NAME_RELEASE,"Continuation")
    @JvmField val result = getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, "Result")
    @JvmField val sequence = IdSignature.PublicSignature("kotlin.sequences", "Sequence", null, 0)
}

private fun IrType.isNotNullClassType(signature: IdSignature.PublicSignature) = isClassType(signature, hasQuestionMark = false)
private fun IrType.isNullableClassType(signature: IdSignature.PublicSignature) = isClassType(signature, hasQuestionMark = true)

fun getPublicSignature(packageFqName: FqName, name: String) =
    IdSignature.PublicSignature(packageFqName.asString(), name, null, 0)

private fun IrType.isClassType(signature: IdSignature.PublicSignature, hasQuestionMark: Boolean? = null): Boolean {
    if (this !is IrSimpleType) return false
    if (hasQuestionMark != null && this.hasQuestionMark != hasQuestionMark) return false
    return signature == classifier.signature
}

fun IrClassifierSymbol.isClassWithFqName(fqName: FqNameUnsafe): Boolean =
    this is IrClassSymbol && classFqNameEquals(this, fqName)

private fun classFqNameEquals(symbol: IrClassSymbol, fqName: FqNameUnsafe): Boolean {
    assert(symbol.isBound)
    return classFqNameEquals(symbol.owner, fqName)
}

private val idSignatureToPrimitiveType: Map<IdSignature.PublicSignature, PrimitiveType> =
    newHashMapWithExpectedSize<IdSignature.PublicSignature, PrimitiveType>(PrimitiveType.values().size).apply {
        for (primitiveType in PrimitiveType.values()) {
            this[getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, primitiveType.typeName.asString())] = primitiveType
        }
    }

val primitiveArrayTypesSignatures: Map<PrimitiveType, IdSignature.PublicSignature> =
    newHashMapWithExpectedSize<PrimitiveType, IdSignature.PublicSignature>(PrimitiveType.values().size).apply {
        for (primitiveType in PrimitiveType.values()) {
            this[primitiveType] =
                getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, "${primitiveType.typeName.asString()}Array")
        }
    }

private fun classFqNameEquals(declaration: IrClass, fqName: FqNameUnsafe): Boolean =
    declaration.hasEqualFqName(fqName.toSafe())

fun IrType.isAny(): Boolean = isNotNullClassType(IdSignatureValues.any)
fun IrType.isNullableAny(): Boolean = isNullableClassType(IdSignatureValues.any)

fun IrType.isString(): Boolean = isNotNullClassType(IdSignatureValues.string)
fun IrType.isNullableString(): Boolean = isNullableClassType(IdSignatureValues.string)
fun IrType.isStringClassType(): Boolean = isClassType(IdSignatureValues.string)
fun IrType.isArray(): Boolean = isNotNullClassType(IdSignatureValues.array)
fun IrType.isNullableArray(): Boolean = isNullableClassType(IdSignatureValues.array)
fun IrType.isCollection(): Boolean = isNotNullClassType(IdSignatureValues.collection)
fun IrType.isNothing(): Boolean = isNotNullClassType(IdSignatureValues.nothing)

fun IrType.isPrimitiveType(hasQuestionMark: Boolean = false): Boolean =
    (this is IrSimpleType && hasQuestionMark == this.hasQuestionMark) &&
            classOrNull?.signature in idSignatureToPrimitiveType

fun IrType.isNullablePrimitiveType(): Boolean = isPrimitiveType(true)

fun IrType.isMarkedNullable() = (this as? IrSimpleType)?.hasQuestionMark ?: false

fun IrType.isUnit() = isNotNullClassType(IdSignatureValues.unit)

fun IrType.isBoolean(): Boolean = isNotNullClassType(IdSignatureValues._boolean)
fun IrType.isChar(): Boolean = isNotNullClassType(IdSignatureValues._char)
fun IrType.isByte(): Boolean = isNotNullClassType(IdSignatureValues._byte)
fun IrType.isShort(): Boolean = isNotNullClassType(IdSignatureValues._short)
fun IrType.isInt(): Boolean = isNotNullClassType(IdSignatureValues._int)
fun IrType.isLong(): Boolean = isNotNullClassType(IdSignatureValues._long)
fun IrType.isUByte(): Boolean = isNotNullClassType(IdSignatureValues.uByte)
fun IrType.isUShort(): Boolean = isNotNullClassType(IdSignatureValues.uShort)
fun IrType.isUInt(): Boolean = isNotNullClassType(IdSignatureValues.uInt)
fun IrType.isULong(): Boolean = isNotNullClassType(IdSignatureValues.uLong)
fun IrType.isFloat(): Boolean = isNotNullClassType(IdSignatureValues._float)
fun IrType.isDouble(): Boolean = isNotNullClassType(IdSignatureValues._double)
fun IrType.isNumber(): Boolean = isNotNullClassType(IdSignatureValues.number)

fun IrType.isComparable(): Boolean = isNotNullClassType(IdSignatureValues.comparable)
fun IrType.isCharSequence(): Boolean = isNotNullClassType(IdSignatureValues.charSequence)
fun IrType.isIterable(): Boolean = isNotNullClassType(IdSignatureValues.iterable)
fun IrType.isSequence(): Boolean = isNotNullClassType(IdSignatureValues.sequence)

fun IrType.isBooleanArray(): Boolean = isNotNullClassType(primitiveArrayTypesSignatures[PrimitiveType.BOOLEAN]!!)
fun IrType.isCharArray(): Boolean = isNotNullClassType(primitiveArrayTypesSignatures[PrimitiveType.CHAR]!!)
fun IrType.isByteArray(): Boolean = isNotNullClassType(primitiveArrayTypesSignatures[PrimitiveType.BYTE]!!)
fun IrType.isShortArray(): Boolean = isNotNullClassType(primitiveArrayTypesSignatures[PrimitiveType.SHORT]!!)
fun IrType.isIntArray(): Boolean = isNotNullClassType(primitiveArrayTypesSignatures[PrimitiveType.INT]!!)
fun IrType.isLongArray(): Boolean = isNotNullClassType(primitiveArrayTypesSignatures[PrimitiveType.LONG]!!)
fun IrType.isFloatArray(): Boolean = isNotNullClassType(primitiveArrayTypesSignatures[PrimitiveType.FLOAT]!!)
fun IrType.isDoubleArray(): Boolean = isNotNullClassType(primitiveArrayTypesSignatures[PrimitiveType.DOUBLE]!!)

// TODO: remove this method using FqNames.
//  Need to refactor declarationBuilders.kt: visibilty is known, need to add info about package in IrFactory.buildClass (similar to name).
fun IrType.isClassType(fqName: FqNameUnsafe, hasQuestionMark: Boolean): Boolean {
    if (this !is IrSimpleType) return false
    if (this.hasQuestionMark != hasQuestionMark) return false
    return classifier.isClassWithFqName(fqName)
}

fun IrType.isKotlinResult(): Boolean = isClassType(StandardNames.RESULT_FQ_NAME.toUnsafe(), false)

fun IrType.isNullableContinuation(): Boolean = isClassType(StandardNames.CONTINUATION_INTERFACE_FQ_NAME_RELEASE.toUnsafe(), true)

// FIR and backend instances have different mask.
fun IrType.isKClass(): Boolean = isClassType(StandardNames.FqNames.kClass, false)
