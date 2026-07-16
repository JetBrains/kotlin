/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.ir.backend.js.utils.getInlineClassUnderlyingType
import org.jetbrains.kotlin.ir.backend.js.utils.isInlineClass
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isNullable

/**
 * Encodes an [IrType] as a short string for use in structural dedup keys.
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
        // Only reached for value classes that ultimately wrap a primitive (reference-wrapping
        // value classes are erased to anyNType by eraseIfReferenceType, so they land in "A").
        // Use the FQN to avoid clashes between same-named classes in different packages.
        "V${this.classOrNull?.owner?.fqNameWhenAvailable?.asString()?.replace('.', '_') ?: this.classOrNull?.owner?.name?.asString() ?: "0"}"
    }
    else -> "A" // anyNType (reference type)
}

internal fun IrType.eraseIfReferenceType(anyNType: IrType): IrType {
    if (this.isPrimitiveType() || this.isUnsignedType()) return this

    // Nullable types are boxed in Wasm, so they must be treated as reference types to avoid merging
    // with the non-nullable variant. This applies particularly for the case of value classes which
    // get inlined later.
    if (this.isNullable()) return anyNType
    if (this.classifierOrNull is IrTypeParameterSymbol) {
        // At the Wasm level, type parameters are passed as their upper bound, so erase them now
        // to merge properly.
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
 * Returns a type-correct zero/null default value expression for `type`.
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
    else -> {
        // Inline classes wrapping a primitive are preserved by eraseIfReferenceType and are
        // represented unboxed in Wasm, so a null default would not typecheck. Emit the zero
        // value of the underlying type and reinterpret it back to the inline class type.
        val inlineClass = type.getClass()?.takeIf { it.isInlineClass }
        if (inlineClass != null) {
            val underlying = defaultValueForType(getInlineClassUnderlyingType(inlineClass), builder)
            IrTypeOperatorCallImpl(
                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                type, IrTypeOperator.REINTERPRET_CAST, type, underlying
            )
        } else {
            builder.irNull()
        }
    }
}
