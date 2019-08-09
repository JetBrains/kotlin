/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ast

class WasmModule(
    val fields: List<WasmModuleField>
) {
    fun toWat(): String =
        "(module\n${fields.joinToString("") { it.toWat() + "\n" }})"
}

sealed class WasmModuleField {
    abstract fun toWat(): String
}

class WasmFunction(
    val name: String,
    val parameters: List<WasmParameter>,
    val returnType: WasmValueType,
    val locals: List<WasmLocal>,
    val instructions: List<WasmInstruction>
) : WasmModuleField() {
    override fun toWat(): String {
        val watId = "$$name"
        val watLocals = locals.joinToString("") { "   " + it.toWat() + "\n" }
        val watParameters = parameters.joinToString(" ") { it.toWat() }
        val watResult = "(result $returnType)"
        val watBody = instructions.joinToString("") { it.toWat("    ") + "\n" }
        return "  (func $watId $watParameters $watResult\n$watLocals$watBody  )"
    }
}

sealed class WasmInstruction {
    abstract fun toWat(ident: String = ""): String
}
class WasmNop : WasmInstruction() {
    override fun toWat(ident: String): String = "$ident(nop)"
}
class WasmReturn(val value: WasmInstruction?): WasmInstruction() {
    override fun toWat(ident: String): String {
        val watValue = value?.toWat("") ?: ""
        return "$ident(return $watValue)"
    }
}

class WasmCall(val name: String, val parameters: List<WasmInstruction>): WasmInstruction() {
    override fun toWat(ident: String): String {
        val arguments = parameters.joinToString(" ") { it.toWat("") }
        return "$ident(call $$name $arguments)"
    }
}


class WasmGetLocal(val name: String): WasmInstruction() {
    override fun toWat(ident: String): String {
        return "$ident(get_local $$name)"
    }
}

class WasmGetGlobal(val name: String): WasmInstruction() {
    override fun toWat(ident: String): String {
        return "$ident(get_global $$name)"
    }
}

class WasmSetGlobal(
    val name: String,
    val instruction: WasmInstruction
): WasmInstruction() {
    override fun toWat(ident: String): String {
        return "$ident(set_global $$name ${instruction.toWat("")})"
    }
}

class WasmSetLocal(
    val name: String,
    val instruction: WasmInstruction
): WasmInstruction() {
    override fun toWat(ident: String): String {
        return "$ident(set_local $$name ${instruction.toWat("")})"
    }
}




sealed class WasmConst<KotlinType, WasmType : WasmValueType>(
    val value: KotlinType,
    val type: WasmType
): WasmInstruction() {
    override fun toWat(ident: String): String {
        return "$ident(${type.wat}.const $value)"
    }
}

class WasmI32Const(value: Int) : WasmConst<Int, WasmI32>(value, WasmI32)
class WasmI64Const(value: Long) : WasmConst<Long, WasmI64>(value, WasmI64)
class WasmF32Const(value: Float) : WasmConst<Float, WasmF32>(value, WasmF32)
class WasmF64Const(value: Double) : WasmConst<Double, WasmF64>(value, WasmF64)



class WasmParameter(
    val name: String,
    val type: WasmValueType
) {
    fun toWat(): String =
        "(param $$name $type)"
}

class WasmLocal(
    val name: String,
    val type: WasmValueType
) {
    fun toWat(): String =
        "(local $$name $type)"
}


class WasmGlobal(
    val name: String,
    val type: WasmValueType,
    val isMutable: Boolean,
    val init: WasmInstruction?
): WasmModuleField() {
    override fun toWat(): String {
        val watMut = if (isMutable) "mut " else ""
        val watInit = if (init != null) " " + init.toWat("") else ""
        return "  (global $$name ($watMut$type) $watInit)"
    }
}

class WasmBody {
    fun toWat(): String = "TODO: Body"
}
class WasmImport

enum class WasmExportKind(val identifier: String) {
    FUNCTION("func"),
    GLOBAL("global")
}

class WasmExport(
    val wasmName: String,
    val exportedName: String,
    val kind: WasmExportKind
) : WasmModuleField() {
    override fun toWat(): String {
        return "  (export \"$exportedName\" (${kind.identifier} $$wasmName))"
    }
}

sealed class WasmValueType(val wat: String) {
    override fun toString(): String {
        return wat
    }
}
object WasmI32 : WasmValueType("i32")
object WasmI64 : WasmValueType("i64")
object WasmF32 : WasmValueType("f32")
object WasmF64 : WasmValueType("f64")

class WasmFunctionType(
    val parameters: List<WasmValueType>,
    val result: WasmValueType
)