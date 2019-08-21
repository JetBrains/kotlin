/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ast


sealed class WasmImmediate {
    object None : WasmImmediate()
    class DeclarationReference(val name: String) : WasmImmediate()
    class StructFieldReference(val name1: String, val name2: Int) : WasmImmediate()
    class LiteralValue<T : Number>(val value: T) : WasmImmediate()
    class ResultType(val type: WasmValueType?) : WasmImmediate()
    class Type2(val t1: WasmValueType, val t2: WasmValueType) : WasmImmediate()
}

sealed class WasmInstruction(
    val mnemonic: String,
    val immediate: WasmImmediate = WasmImmediate.None,
    val operands: List<WasmInstruction> = emptyList()
)

class WasmSimpleInstruction(mnemonic: String, operands: List<WasmInstruction>) :
    WasmInstruction(mnemonic, operands = operands)

object WasmNop : WasmInstruction("nop")

object WasmUnreachable : WasmInstruction("unreachable")

object WasmRefNull : WasmInstruction("ref.null")

class WasmReturn(value: WasmInstruction? = null) :
    WasmInstruction("return", operands = listOfNotNull(value))

class WasmDrop(instructions: List<WasmInstruction>) :
    WasmInstruction("drop", operands = instructions)

class WasmNot(boolValue: WasmInstruction) :
    WasmInstruction("i32.eqz", operands = listOf(boolValue))

class WasmCall(name: String, operands: List<WasmInstruction>) :
    WasmInstruction("call", WasmImmediate.DeclarationReference(name), operands)

// Last operand is a function index
class WasmCallIdirect(functionType: String, operands: List<WasmInstruction>) :
    WasmInstruction("call_indirect", immediate = WasmImmediate.DeclarationReference(functionType), operands = operands)

class WasmGetLocal(name: String) :
    WasmInstruction("get_local", WasmImmediate.DeclarationReference(name))

class WasmGetGlobal(name: String) :
    WasmInstruction("get_global", WasmImmediate.DeclarationReference(name))

class WasmSetGlobal(name: String, value: WasmInstruction) :
    WasmInstruction("set_global", WasmImmediate.DeclarationReference(name), listOf(value))

class WasmSetLocal(name: String, value: WasmInstruction) :
    WasmInstruction("set_local", WasmImmediate.DeclarationReference(name), listOf(value))

class WasmIf(condition: WasmInstruction, resultType: WasmValueType?, thenInstructions: WasmThen?, elseInstruction: WasmElse?) :
    WasmInstruction(
        "if",
        immediate = WasmImmediate.ResultType(resultType),
        operands = listOfNotNull(condition, thenInstructions, elseInstruction)
    )

class WasmBrIf(condition: WasmInstruction, label: String) :
    WasmInstruction(
        "br_if",
        immediate = WasmImmediate.DeclarationReference(label),
        operands = listOfNotNull(condition)
    )


class WasmLoop(label: String, body: List<WasmInstruction>) :
    WasmInstruction(
        "loop",
        immediate = WasmImmediate.DeclarationReference(label),
        operands = body
    )

class WasmBr(label: String) :
    WasmInstruction(
        "br",
        immediate = WasmImmediate.DeclarationReference(label)
    )


class WasmThen(insts: List<WasmInstruction>) :
    WasmInstruction("then", operands = insts)

class WasmElse(insts: List<WasmInstruction>) :
    WasmInstruction("else", operands = insts)

class WasmBlock(instructions: List<WasmInstruction>, resultType: WasmValueType?) :
    WasmInstruction("block", immediate = WasmImmediate.ResultType(resultType), operands = instructions)

class WasmLabelledBlock(label: String, instructions: List<WasmInstruction>) :
    WasmInstruction("block", immediate = WasmImmediate.DeclarationReference(label), operands = instructions)


class WasmStructNew(structName: String, operands: List<WasmInstruction>) :
    WasmInstruction("struct.new", WasmImmediate.DeclarationReference(structName), operands)

class WasmStructSet(structName: String, fieldId: Int, structRef: WasmInstruction, value: WasmInstruction) :
    WasmInstruction("struct.set", WasmImmediate.StructFieldReference(structName, fieldId), listOf(structRef, value))

class WasmStructGet(structName: String, fieldId: Int, structRef: WasmInstruction) :
    WasmInstruction("struct.get", WasmImmediate.StructFieldReference(structName, fieldId), listOf(structRef))

class WasmStructNarrow(fromType: WasmValueType, toType: WasmValueType, value: WasmInstruction) :
    WasmInstruction("struct.narrow", WasmImmediate.Type2(fromType, toType), listOf(value))

sealed class WasmConst<KotlinType : Number, WasmType : WasmValueType>(value: KotlinType, type: WasmType) :
    WasmInstruction(type.mnemonic + ".const", WasmImmediate.LiteralValue<KotlinType>(value))

class WasmI32Const(value: Int) : WasmConst<Int, WasmI32>(value, WasmI32)
class WasmI64Const(value: Long) : WasmConst<Long, WasmI64>(value, WasmI64)
class WasmF32Const(value: Float) : WasmConst<Float, WasmF32>(value, WasmF32)
class WasmF64Const(value: Double) : WasmConst<Double, WasmF64>(value, WasmF64)
