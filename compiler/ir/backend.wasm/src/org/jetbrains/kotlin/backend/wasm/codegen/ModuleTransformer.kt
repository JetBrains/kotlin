/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.backend.wasm.ast.WasmExport
import org.jetbrains.kotlin.backend.wasm.ast.WasmModule
import org.jetbrains.kotlin.backend.wasm.ast.wasmModuleToWat
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.jsAssignment
import org.jetbrains.kotlin.ir.backend.js.utils.sanitizeName
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.js.backend.ast.JsArrayLiteral
import org.jetbrains.kotlin.js.backend.ast.JsBlock
import org.jetbrains.kotlin.js.backend.ast.JsNameRef
import org.jetbrains.kotlin.js.backend.ast.JsStringLiteral
import org.jetbrains.kotlin.utils.addIfNotNull

class IrModuleToWasm(private val backendContext: WasmBackendContext) {
    fun generateModule(module: IrModuleFragment): WasmCompilerResult {
        val nameTable = generateWatTopLevelNames(module.files)
        val context = WasmCodegenContext(nameTable, backendContext)
        val irDeclarations = module.files.flatMap { it.declarations }
        val wasmDeclarations = irDeclarations.mapNotNull { it.accept(DeclarationTransformer(), context) }
        val exports = generateExports(module, context)


        val wasmModule = WasmModule(context.imports + wasmDeclarations + exports)
        val wat = wasmModuleToWat(wasmModule)
        return WasmCompilerResult(wat, generateStringLiteralsSupport(context.stringLiterals))
    }

    private fun generateStringLiteralsSupport(literals: List<String>): String {
        return JsBlock(
            jsAssignment(
                JsNameRef("stringLiterals", "runtime"),
                JsArrayLiteral(literals.map { JsStringLiteral(it) })
            ).makeStmt()
        ).toString()
    }

    private fun generateExports(module: IrModuleFragment, context: WasmCodegenContext): List<WasmExport> {
        val exports = mutableListOf<WasmExport>()
        for (file in module.files) {
            for (declaration in file.declarations) {
                exports.addIfNotNull(generateExport(declaration, context))
            }
        }
        return exports
    }

    private fun generateExport(declaration: IrDeclaration, context: WasmCodegenContext): WasmExport? {
        if (declaration !is IrDeclarationWithVisibility ||
            declaration !is IrDeclarationWithName ||
            declaration !is IrSimpleFunction ||
            declaration.visibility != Visibilities.PUBLIC
        ) {
            return null
        }

        if (!declaration.isExported(context))
            return null

        val internalName = context.getGlobalName(declaration)
        val exportedName = sanitizeName(declaration.name.identifier)

        return WasmExport(
            wasmName = internalName,
            exportedName = exportedName,
            kind = WasmExport.Kind.FUNCTION
        )
    }

}

fun IrFunction.isExported(context: WasmCodegenContext): Boolean =
    fqNameWhenAvailable in context.backendContext.additionalExportedDeclarations
