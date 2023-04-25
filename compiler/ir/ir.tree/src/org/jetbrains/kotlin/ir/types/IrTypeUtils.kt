/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.types.AbstractTypeChecker

fun IrClassifierSymbol.superTypes(): List<IrType> = when (this) {
    is IrClassSymbol -> owner.superTypes
    is IrTypeParameterSymbol -> owner.superTypes
    else -> emptyList()
}

fun IrClassifierSymbol.isSubtypeOfClass(superClass: IrClassSymbol): Boolean =
    FqNameEqualityChecker.areEqual(this, superClass) || isStrictSubtypeOfClass(superClass)

fun IrClassifierSymbol.isStrictSubtypeOfClass(superClass: IrClassSymbol): Boolean =
    superTypes().any { it.isSubtypeOfClass(superClass) }

fun IrType.isSubtypeOfClass(superClass: IrClassSymbol): Boolean =
    this is IrSimpleType && classifier.isSubtypeOfClass(superClass)

fun IrType.isStrictSubtypeOfClass(superClass: IrClassSymbol): Boolean =
    this is IrSimpleType && classifier.isStrictSubtypeOfClass(superClass)

fun IrType.isSubtypeOf(superType: IrType, typeSystem: IrTypeSystemContext): Boolean =
    AbstractTypeChecker.isSubtypeOf(createIrTypeCheckerState(typeSystem), this, superType)

fun IrType.isNullable(): Boolean =
    when (this) {
        is IrSimpleType -> when (val classifier = classifier) {
            is IrClassSymbol -> nullability == SimpleTypeNullability.MARKED_NULLABLE
            is IrTypeParameterSymbol -> when (nullability) {
                SimpleTypeNullability.MARKED_NULLABLE -> true
                // here is a bug, there should be .all check (not .any),
                // but fixing it is a breaking change, see KT-31545 for details
                SimpleTypeNullability.NOT_SPECIFIED -> classifier.owner.superTypes.any(IrType::isNullable)
                SimpleTypeNullability.DEFINITELY_NOT_NULL -> false
            }
            is IrScriptSymbol -> nullability == SimpleTypeNullability.MARKED_NULLABLE
            else -> error("Unsupported classifier: $classifier")
        }
        is IrDynamicType -> true
        is IrErrorType -> this.isMarkedNullable
        else -> false
    }

val IrType.isBoxedArray: Boolean
    get() = classOrNull?.owner?.fqNameWhenAvailable == StandardNames.FqNames.array.toSafe()

fun IrType.getArrayElementType(irBuiltIns: IrBuiltIns): IrType =
    if (isBoxedArray) {
        when (val argument = (this as IrSimpleType).arguments.singleOrNull()) {
            is IrTypeProjection ->
                argument.type
            is IrStarProjection ->
                irBuiltIns.anyNType
            null ->
                error("Unexpected array argument type: null")
        }
    } else {
        val classifier = this.classOrNull!!
        irBuiltIns.primitiveArrayElementTypes[classifier]
            ?: irBuiltIns.unsignedArraysElementTypes[classifier]
            ?: throw AssertionError("Primitive array expected: $classifier")
    }

fun IrType.toArrayOrPrimitiveArrayType(irBuiltIns: IrBuiltIns): IrType =
    if (isPrimitiveType()) {
        irBuiltIns.primitiveArrayForType[this]?.defaultType
            ?: throw AssertionError("$this not in primitiveArrayForType")
    } else {
        irBuiltIns.arrayClass.typeWith(this)
    }
