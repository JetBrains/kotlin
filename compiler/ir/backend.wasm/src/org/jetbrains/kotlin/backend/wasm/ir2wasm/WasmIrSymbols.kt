/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.wasm.ir.WasmHeapType.Type
import org.jetbrains.kotlin.wasm.ir.WasmImmediate

class FuncSymbol(val value: IdSignature) : WasmImmediate.FuncIdx()

class FieldGlobalSymbol(val value: IdSignature) : WasmImmediate.GlobalIdx()
class VTableGlobalSymbol(val value: IdSignature) : WasmImmediate.GlobalIdx()
class ClassITableGlobalSymbol(val value: IdSignature) : WasmImmediate.GlobalIdx()
class RttiGlobalSymbol(val value: IdSignature) : WasmImmediate.GlobalIdx()
class LiteralGlobalSymbol(val value: String) : WasmImmediate.GlobalIdx() {
    override fun equals(other: Any?): Boolean = other is LiteralGlobalSymbol && value == other.value
    override fun hashCode(): Int = value.hashCode()
}

class GcTypeSymbol(val value: IdSignature) : WasmImmediate.TypeIdx()
class VTableTypeSymbol(val value: IdSignature) : WasmImmediate.TypeIdx()
class FunctionTypeSymbol(val value: IdSignature) : WasmImmediate.TypeIdx()

class GcHeapTypeSymbol(val type: IdSignature) : Type.GcType() {
    override fun hashCode(): Int = type.hashCode()
    override fun equals(other: Any?): Boolean = other is GcHeapTypeSymbol && type == other.type
    override fun toString(): String = "GcHeapTypeSymbol:$type"
}

class VTableHeapTypeSymbol(val type: IdSignature) : Type.VTableType() {
    override fun hashCode(): Int = type.hashCode()
    override fun equals(other: Any?): Boolean = other is VTableHeapTypeSymbol && type == other.type
    override fun toString(): String = "VTableHeapTypeSymbol:$type"
}

class FunctionHeapTypeSymbol(val type: IdSignature) : Type.FunctionType() {
    override fun hashCode(): Int = type.hashCode()
    override fun equals(other: Any?): Boolean = other is FunctionHeapTypeSymbol && type == other.type
    override fun toString(): String = "FunctionHeapTypeSymbol:$type"
}