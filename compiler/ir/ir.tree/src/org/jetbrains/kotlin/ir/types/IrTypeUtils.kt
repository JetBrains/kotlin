/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.isStrictSubtypeOfClass
import org.jetbrains.kotlin.ir.util.isSubtypeOf
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.getArrayElementType
import org.jetbrains.kotlin.ir.util.toArrayOrPrimitiveArrayType
import org.jetbrains.kotlin.ir.util.isBoxedArray

@Deprecated("Use `kotlin.ir.util.superTypes` instead", ReplaceWith("kotlin.ir.util.superTypes"))
fun IrClassifierSymbol.superTypes(): List<IrType> =
    superTypes()

@Deprecated("Use `kotlin.ir.util.isSubtypeOfClass` instead", ReplaceWith("kotlin.ir.util.isSubtypeOfClass"))
fun IrClassifierSymbol.isSubtypeOfClass(superClass: IrClassSymbol): Boolean =
    isSubtypeOfClass(superClass)

@Deprecated("Use `kotlin.ir.util.isStrictSubtypeOfClass` instead", ReplaceWith("kotlin.ir.util.isStrictSubtypeOfClass"))
fun IrClassifierSymbol.isStrictSubtypeOfClass(superClass: IrClassSymbol): Boolean =
    isStrictSubtypeOfClass(superClass)

@Deprecated("Use `kotlin.ir.util.isSubtypeOfClass` instead", ReplaceWith("kotlin.ir.util.isSubtypeOfClass"))
fun IrType.isSubtypeOfClass(superClass: IrClassSymbol): Boolean =
    isSubtypeOfClass(superClass)

@Deprecated("Use `kotlin.ir.util.isStrictSubtypeOfClass` instead", ReplaceWith("kotlin.ir.util.isStrictSubtypeOfClass"))
fun IrType.isStrictSubtypeOfClass(superClass: IrClassSymbol): Boolean =
    isStrictSubtypeOfClass(superClass)

@Deprecated("Use `kotlin.ir.util.isSubtypeOf` instead", ReplaceWith("kotlin.ir.util.isSubtypeOf"))
fun IrType.isSubtypeOf(superType: IrType, typeSystem: IrTypeSystemContext): Boolean =
    isSubtypeOf(superType, typeSystem)

@Deprecated("Use `kotlin.ir.util.isNullable` instead", ReplaceWith("kotlin.ir.util.isNullable"))
fun IrType.isNullable(): Boolean =
    isNullable()

@Deprecated("Use `kotlin.ir.util.isBoxedArray` instead", ReplaceWith("kotlin.ir.util.isBoxedArray"))
val IrType.isBoxedArray: Boolean by IrType::isBoxedArray

@Deprecated("Use `kotlin.ir.util.getArrayElementType` instead", ReplaceWith("kotlin.ir.util.getArrayElementType"))
fun IrType.getArrayElementType(irBuiltIns: IrBuiltIns): IrType =
    getArrayElementType(irBuiltIns)

@Deprecated("Use `kotlin.ir.util.toArrayOrPrimitiveArrayType` instead", ReplaceWith("kotlin.ir.util.toArrayOrPrimitiveArrayType"))
fun IrType.toArrayOrPrimitiveArrayType(irBuiltIns: IrBuiltIns): IrType =
    toArrayOrPrimitiveArrayType(irBuiltIns)
