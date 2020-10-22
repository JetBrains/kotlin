/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.utils

import org.jetbrains.kotlin.ir.backend.js.utils.getSingleConstStringArgument
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.wasm.ir.WasmImportPair

fun IrAnnotationContainer.hasExcludedFromCodegenAnnotation(): Boolean =
    hasAnnotation(FqName("kotlin.wasm.internal.ExcludedFromCodegen"))

fun IrAnnotationContainer.getWasmOpAnnotation(): String? =
    getAnnotation(FqName("kotlin.wasm.internal.WasmOp"))?.getSingleConstStringArgument()

fun IrAnnotationContainer.hasWasmReinterpretAnnotation(): Boolean =
    hasAnnotation(FqName("kotlin.wasm.internal.WasmReinterpret"))

fun IrAnnotationContainer.hasWasmForeignAnnotation(): Boolean =
    hasAnnotation(FqName("kotlin.wasm.internal.WasmForeign"))

fun IrAnnotationContainer.hasWasmPrimitiveAnnotation(): Boolean =
    hasAnnotation(FqName("kotlin.wasm.internal.WasmPrimitive"))

fun IrAnnotationContainer.getWasmImportAnnotation(): WasmImportPair? =
    getAnnotation(FqName("kotlin.wasm.internal.WasmImport"))?.let {
        WasmImportPair(
            (it.getValueArgument(0) as IrConst<*>).value as String,
            (it.getValueArgument(1) as IrConst<*>).value as String
        )
    }
