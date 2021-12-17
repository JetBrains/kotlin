/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.FqNameEqualityChecker
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
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

fun IrType.isSubtypeOf(superType: IrType, typeSystem: IrTypeSystemContext): Boolean =
    AbstractTypeChecker.isSubtypeOf(createIrTypeCheckerState(typeSystem), this, superType)

@Suppress("DEPRECATION_ERROR")
@Deprecated("rhizomedb & noria compatibility", level = DeprecationLevel.ERROR)
fun IrType.isSubtypeOf(superType: IrType, irBuiltIns: org.jetbrains.kotlin.ir.descriptors.IrBuiltIns): Boolean =
    isSubtypeOf(superType, IrTypeSystemContextImpl(irBuiltIns.irBuiltIns))

fun IrType.isNullable(): Boolean =
    when (this) {
        is IrDefinitelyNotNullType -> false
        is IrSimpleType -> when (val classifier = classifier) {
            is IrClassSymbol -> hasQuestionMark
            is IrTypeParameterSymbol -> hasQuestionMark || classifier.owner.superTypes.any(IrType::isNullable)
            else -> error("Unsupported classifier: $classifier")
        }
        is IrDynamicType -> true
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
            else ->
                error("Unexpected array argument type: $argument")
        }
    } else {
        val classifier = this.classOrNull!!
        irBuiltIns.primitiveArrayElementTypes[classifier]
            ?: throw AssertionError("Primitive array expected: $classifier")
    }

fun IrType.toArrayOrPrimitiveArrayType(irBuiltIns: IrBuiltIns): IrType =
    if (isPrimitiveType()) {
        irBuiltIns.primitiveArrayForType[this]?.defaultType
            ?: throw AssertionError("$this not in primitiveArrayForType")
    } else {
        irBuiltIns.arrayClass.typeWith(this)
    }
