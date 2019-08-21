/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ast

// TODO: Abstract out S-expression part of dumping?

fun WasmInstruction.toWat(ident: String = ""): String =
    "$ident($mnemonic${immediate.toWat()}${operands.joinToString("") { "\n" + it.toWat("$ident  ") }})"

fun WasmImmediate.toWat(): String = when (this) {
    WasmImmediate.None -> ""
    is WasmImmediate.DeclarationReference -> " $$name"
    // SpiderMonkey jsshell won't parse Uppercase letters in literals
    is WasmImmediate.LiteralValue<*> -> " $value".toLowerCase()
    is WasmImmediate.ResultType -> type?.let { " (result ${type.mnemonic})" } ?: ""
    is WasmImmediate.StructFieldReference -> " $$name1 $name2"
    is WasmImmediate.Type2 -> " " + t1.mnemonic + " " + t2.mnemonic
}

fun wasmModuleToWat(module: WasmModule): String =
    "(module\n ${module.fields.joinToString("") { wasmModuleFieldToWat(it) + "\n" }})"

fun wasmFunctionToWat(function: WasmFunction): String {
    val watId = "$${function.name}"
    val watImport = function.importPair?.let { importPair ->
        " (import ${toWasString(importPair.module)} ${toWasString(importPair.name)})"
    } ?: ""
    val watLocals = function.locals.joinToString("") { "   " + wasmLocalToWat(it) + "\n" }
    val watParameters = function.parameters.joinToString("") { " " + wasmParameterToWat(it, function.importPair == null) }
    val watResult = function.returnType?.let { type -> " (result ${type.mnemonic})" } ?: ""
    val watBody = function.instructions.joinToString("") { it.toWat("    ") + "\n" }
    return "  (func $watId$watImport$watParameters$watResult\n$watLocals$watBody  )"
}

fun wasmParameterToWat(parameter: WasmParameter, includeName: Boolean): String {
    val name = if (includeName) " $${parameter.name}" else ""
    return "(param$name ${parameter.type.mnemonic})"
}

fun wasmLocalToWat(local: WasmLocal): String =
    local.run { "(local $$name ${type.mnemonic})" }

fun wasmGlobalToWat(global: WasmGlobal): String {
    val watMut = if (global.isMutable) "mut " else ""
    val watInit = global.init?.toWat("") ?: ""
    return global.run { "  (global $$name ($watMut${type.mnemonic}) $watInit)" }
}

fun wasmExportToWat(export: WasmExport): String =
    export.run { "  (export \"$exportedName\" (${kind.keyword} $$wasmName))" }

fun wasmStartToWat(start: WasmStart): String =
    start.run { "  (start $${start.name})" }

fun wasmModuleFieldToWat(moduleField: WasmModuleField): String =
    when (moduleField) {
        is WasmFunction -> wasmFunctionToWat(moduleField)
        is WasmGlobal -> wasmGlobalToWat(moduleField)
        is WasmExport -> wasmExportToWat(moduleField)
        is WasmStart -> wasmStartToWat(moduleField)
        is WasmModuleFieldList -> moduleField.fields.joinToString("") { wasmModuleFieldToWat(it) + "\n" }
        is WasmFunctionType -> wasmFunctionTypeDeclarationToWat(moduleField)
        is WasmStructType -> wasmStructTypeDeclarationToWat(moduleField)
        is WasmData -> wasmDataToWat(moduleField)
        is WasmFuncrefTable -> wasmFuncRefTableToWat(moduleField)
        is WasmMemory -> wasmMemoryToWat(moduleField)
        is WasmCustomSection -> moduleField.wat
    }

fun wasmMemoryToWat(memory: WasmMemory): String =
    memory.run { "  (memory $minSize $maxSize)" }

fun wasmFuncRefTableToWat(table: WasmFuncrefTable): String =
    " (table funcref \n  (elem ${table.functions.joinToString("") { "\n    $$it" }}))"

fun wasmDataToWat(data: WasmData): String =
    data.run { " (data (i32.const ${data.offset}) ${toWasString(data.bytes.toWatData())})" }

fun wasmStructTypeDeclarationToWat(structType: WasmStructType): String =
    structType.run { "  (type $$name (struct ${fields.joinToString(" ") { it.toWat() }}))" }

private fun WasmStructField.toWat(): String =
    "(field ${if (isMutable) "(mut ${type.mnemonic})" else type.mnemonic})"

fun wasmFunctionTypeDeclarationToWat(functionType: WasmFunctionType): String =
    functionType.run {
        "  (type $$name (func (param ${parameters.joinToString(" ") { it.mnemonic }}) ${result?.let { "(result ${it.mnemonic})" } ?: ""}))"
    }

fun toWasString(s: String): String {
    // TODO: escape characters according to
    //  https://webassembly.github.io/spec/core/text/values.html#strings
    return "\"" + s + "\""
}


fun Byte.toWatData() = "\\" + this.toUByte().toString(16).padStart(2, '0')
fun ByteArray.toWatData(): String = joinToString("") { it.toWatData() }
