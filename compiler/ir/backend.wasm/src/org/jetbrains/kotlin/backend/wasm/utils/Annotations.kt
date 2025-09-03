/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.utils

import org.jetbrains.kotlin.ir.backend.js.utils.getSingleConstStringArgument
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.wasm.ir.JsBuiltinDescriptor
import org.jetbrains.kotlin.wasm.ir.WasmImportDescriptor
import org.jetbrains.kotlin.wasm.ir.WasmSymbol

private val excludedFromCodegenFqName = FqName("kotlin.wasm.internal.ExcludedFromCodegen")
private val wasmImportFqName: FqName = FqName("kotlin.wasm.WasmImport")
private val wasmOpFqName = FqName("kotlin.wasm.internal.WasmOp")
private val wasmNoOpCastFqName = FqName("kotlin.wasm.internal.WasmNoOpCast")
private val wasmAutoboxedFqName = FqName("kotlin.wasm.internal.WasmAutoboxed")
private val wasmPrimitiveConstructorFqName = FqName("kotlin.wasm.internal.WasmPrimitiveConstructor")
private val wasmArrayOfFqName = FqName("kotlin.wasm.internal.WasmArrayOf")
private val jsFunFqName = FqName("kotlin.JsFun")
private val jsPrimitiveFqName = FqName("kotlin.wasm.internal.JsPrimitive")
private val wasmExportFqName = FqName("kotlin.wasm.WasmExport")
private val jsBuiltinFqName = FqName("kotlin.wasm.internal.JsBuiltin")
private val managedExternrefFqName: FqName = FqName("kotlin.wasm.internal.ManagedExternref")

fun IrAnnotationContainer.hasExcludedFromCodegenAnnotation(): Boolean =
    hasAnnotation(excludedFromCodegenFqName)

fun IrFunction.getWasmImportDescriptor(): WasmImportDescriptor? {
    val annotation = getAnnotation(wasmImportFqName)
        ?: return null

    val moduleName = (annotation.arguments[0] as IrConst).value as String
    val declarationName = (annotation.arguments[1] as? IrConst)?.value as? String
    return WasmImportDescriptor(
        moduleName,
        WasmSymbol(declarationName ?: this.name.asString())
    )
}

fun IrFunction.getJsBuiltinDescriptor(): JsBuiltinDescriptor? {
    val annotation = getAnnotation(jsBuiltinFqName)
        ?: return null

    val moduleName = (annotation.arguments[0] as IrConst).value as String
    val declarationName = (annotation.arguments[1] as? IrConst)?.value as? String
    val polyfillImpl = (annotation.arguments[2] as? IrConst)?.value as String
    return JsBuiltinDescriptor(
        "wasm:$moduleName",
        declarationName ?: this.name.asString(),
        polyfillImpl
    )
}

fun IrAnnotationContainer.getWasmOpAnnotation(): String? =
    getAnnotation(wasmOpFqName)?.getSingleConstStringArgument()

fun IrAnnotationContainer.hasWasmNoOpCastAnnotation(): Boolean =
    hasAnnotation(wasmNoOpCastFqName)

fun IrAnnotationContainer.hasWasmAutoboxedAnnotation(): Boolean =
    hasAnnotation(wasmAutoboxedFqName)

fun IrAnnotationContainer.hasWasmPrimitiveConstructorAnnotation(): Boolean =
    hasAnnotation(wasmPrimitiveConstructorFqName)

fun IrAnnotationContainer.hasManagedExternrefAnnotation(): Boolean =
    hasAnnotation(managedExternrefFqName)

class WasmArrayInfo(val klass: IrClass, val isNullable: Boolean, val isMutable: Boolean) {
    val type = klass.defaultType.let { if (isNullable) it.makeNullable() else it }
}

fun IrAnnotationContainer.getWasmArrayAnnotation(): WasmArrayInfo? =
    getAnnotation(wasmArrayOfFqName)?.let {
        WasmArrayInfo(
            (it.arguments[0] as IrClassReference).symbol.owner as IrClass,
            (it.arguments[1] as IrConst).value as Boolean,
            (it.arguments[2] as? IrConst)?.value as? Boolean ?: true,
        )
    }

fun IrAnnotationContainer.getJsFunAnnotation(): String? =
    getAnnotation(jsFunFqName)?.getSingleConstStringArgument()

fun IrAnnotationContainer.getJsPrimitiveType(): String? =
    getAnnotation(jsPrimitiveFqName)?.getSingleConstStringArgument()

fun IrFunction.getWasmExportNameIfWasmExport(): String? {
    val annotation = getAnnotation(wasmExportFqName) ?: return null
    if (annotation.arguments.isEmpty()) return name.identifier
    val nameFromAnnotation = (annotation.arguments[0] as? IrConst)?.value as? String
    return nameFromAnnotation ?: name.identifier
}