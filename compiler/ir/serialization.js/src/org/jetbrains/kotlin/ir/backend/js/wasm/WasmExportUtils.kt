/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.wasm

import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.collectJsExportNames
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.name.WasmStandardClassIds
import kotlin.sequences.associateWith
import kotlin.sequences.filter

typealias ExportNamesMap = Map<ExportKind, Map<IrFile, Map<IrDeclarationWithName, String>>>

enum class ExportKind {
    JsExport,
    WasmExport
}

fun IrAnnotationContainer.isWasmExportDeclaration(): Boolean {
    return hasAnnotation(WasmStandardClassIds.Annotations.WasmExport.asSingleFqName())
}

fun IrDeclarationWithName.getWasmExportName(): String {
    val annotation = getAnnotation(WasmStandardClassIds.Annotations.WasmExport.asSingleFqName())!!
    val nameFromAnnotation = (annotation.arguments[0] as? IrConst)?.value as? String
    return nameFromAnnotation ?: name.identifier
}

fun IrModuleFragment.collectWasmExportNames(): Map<IrFile, Map<IrDeclarationWithName, String>> =
    files.associateWith { irFile ->
        irFile.declarations.asSequence()
            .filterIsInstance<IrDeclarationWithName>()
            .filter { it.isWasmExportDeclaration() && !it.isEffectivelyExternal() }
            .associateWith { it.getWasmExportName() }
    }

fun IrModuleFragment.collectAllExportNames(): ExportNamesMap =
    mapOf(ExportKind.JsExport to collectJsExportNames(), ExportKind.WasmExport to collectWasmExportNames())