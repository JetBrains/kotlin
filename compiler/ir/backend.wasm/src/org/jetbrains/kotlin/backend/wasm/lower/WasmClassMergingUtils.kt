/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.ir.backend.js.utils.getInlineClassUnderlyingType
import org.jetbrains.kotlin.ir.backend.js.utils.isInlineClass
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isNullable

/**
 * Shared utilities for WASM class merging lowerings.
 * Used by [WasmCallableReferenceLowering] and [WasmSuspendLambdaMergingLowering]
 * to compute structural dedup keys and erase field types.
 */

/**
 * Encodes an [IrType] as a short string for use in structural dedup keys.
 * Primitives and unsigned types get distinct codes; inline classes wrapping primitives
 * use a `V`-prefixed FQN; everything else maps to `"A"` (reference / anyNType).
 */
internal fun IrType.toTypeSignatureCode(): String = when {
    this.isInt() -> "I"
    this.isLong() -> "J"
    this.isFloat() -> "F"
    this.isDouble() -> "D"
    this.isBoolean() -> "Z"
    this.isChar() -> "C"
    this.isByte() -> "B"
    this.isShort() -> "S"
    this.isUInt() -> "UI"
    this.isULong() -> "UJ"
    this.isUByte() -> "UB"
    this.isUShort() -> "US"
    this.getClass()?.isInlineClass == true -> {
        "V${this.classOrNull?.owner?.fqNameWhenAvailable?.asString()?.replace('.', '_') ?: this.classOrNull?.owner?.name?.asString() ?: "0"}"
    }
    else -> "A"
}

/**
 * Erases reference types to [anyNType] for field type deduplication.
 * Primitive and unsigned types are preserved. Nullable types, type parameters,
 * and reference-wrapping inline classes are all erased.
 * Inline classes that ultimately wrap a primitive are preserved.
 */
internal fun IrType.eraseIfReferenceType(anyNType: IrType): IrType {
    if (this.isPrimitiveType() || this.isUnsignedType()) return this
    if (this.isNullable()) return anyNType
    if (this.classifierOrNull is IrTypeParameterSymbol) {
        val typeParam = (this.classifierOrNull as IrTypeParameterSymbol).owner
        return (typeParam.superTypes.firstOrNull() ?: anyNType).eraseIfReferenceType(anyNType)
    }
    val clazz = this.getClass() ?: return anyNType
    if (clazz.isInlineClass) {
        val underlyingErased = getInlineClassUnderlyingType(clazz).eraseIfReferenceType(anyNType)
        return if (underlyingErased.isPrimitiveType() || underlyingErased.isUnsignedType()) {
            this
        } else {
            anyNType
        }
    }
    return anyNType
}

/**
 * Returns a type-correct zero/null default value expression for [type].
 * Used when a shared class has more fields than a particular lambda needs,
 * and the extra fields must be initialized to a valid default.
 */
internal fun defaultValueForType(type: IrType, builder: IrBuilderWithScope): IrExpression = when {
    type.isInt() || type.isUInt() -> builder.irInt(0)
    type.isLong() || type.isULong() -> builder.irLong(0)
    type.isFloat() -> IrConstImpl.float(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, type, 0f)
    type.isDouble() -> IrConstImpl.double(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, type, 0.0)
    type.isBoolean() -> builder.irBoolean(false)
    type.isChar() -> builder.irChar(' ')
    type.isByte() || type.isUByte() -> builder.irByte(0)
    type.isShort() || type.isUShort() -> builder.irShort(0)
    else -> builder.irNull()
}
