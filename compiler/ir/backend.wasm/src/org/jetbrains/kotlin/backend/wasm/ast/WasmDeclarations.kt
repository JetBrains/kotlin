/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ast

import org.jetbrains.kotlin.backend.wasm.utils.WasmImportPair

class WasmModule(
    val fields: List<WasmModuleField>
)

sealed class WasmModuleField

class WasmModuleFieldList(
    val fields: List<WasmModuleField>
) : WasmModuleField()

class WasmFunction(
    val name: String,
    val parameters: List<WasmParameter>,
    val returnType: WasmValueType?,
    val locals: List<WasmLocal>,
    val instructions: List<WasmInstruction>,
    val importPair: WasmImportPair?
) : WasmModuleField()

class WasmParameter(
    val name: String,
    val type: WasmValueType
)

class WasmLocal(
    val name: String,
    val type: WasmValueType
)

class WasmGlobal(
    val name: String,
    val type: WasmValueType,
    val isMutable: Boolean,
    val init: WasmInstruction?
) : WasmModuleField()

class WasmExport(
    val wasmName: String,
    val exportedName: String,
    val kind: Kind
) : WasmModuleField() {
    enum class Kind(val keyword: String) {
        FUNCTION("func"),
        GLOBAL("global")
    }
}
