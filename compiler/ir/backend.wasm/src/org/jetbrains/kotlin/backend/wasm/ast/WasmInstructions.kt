/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ast

sealed class WasmImmediate {
    // TODO: Move away from strings
    class BlockReference(val block: WasmBranchTarget) : WasmImmediate()

    class VariableRef(val local: WasmLocal) : WasmImmediate()

    sealed class FunctionSymbol : WasmImmediate() {
        class Linked(val ref: WasmSymbol<WasmFunction>) : FunctionSymbol()
    }

    sealed class FunctionTypeSymbol : WasmImmediate() {
        class Linked(val ref: WasmSymbol<WasmFunctionType>) : FunctionTypeSymbol()
    }

    sealed class GlobalSymbol : WasmImmediate() {
        class Linked(val ref: WasmSymbol<WasmGlobal>) : GlobalSymbol()
    }

    sealed class StructTypeSymbol : WasmImmediate() {
        class Linked(val ref: WasmSymbol<WasmStructType>) : StructTypeSymbol()
    }

    sealed class StructFieldSymbol : WasmImmediate() {
        class Linked(val ref: WasmSymbol<Int>) : StructFieldSymbol()
    }

    class LiteralValue<T : Number>(val value: T) : WasmImmediate()
    class I32Symbol(val value: WasmSymbol<Int>) : WasmImmediate()
    class ValueType(val type: WasmValueType) : WasmImmediate()
    class ResultType(val type: WasmValueType?) : WasmImmediate()
}

sealed class WasmInstruction(
    val mnemonic: String,
    val immediates: List<WasmImmediate> = emptyList(),
    val operands: List<WasmInstruction> = emptyList()
) {
    val blockBody = mutableListOf<WasmInstruction>()
    constructor(mnemonic: String, immediate: WasmImmediate, operands: List<WasmInstruction> = emptyList()) :
            this(mnemonic, listOf(immediate), operands)
}

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

class WasmCall(symbol: WasmSymbol<WasmFunction>, operands: List<WasmInstruction>) :
    WasmInstruction("call", WasmImmediate.FunctionSymbol.Linked(symbol), operands)

// Last operand is a function index
class WasmCallIndirect(symbol: WasmSymbol<WasmFunctionType>, operands: List<WasmInstruction>) :
    WasmInstruction("call_indirect", immediate = WasmImmediate.FunctionTypeSymbol.Linked(symbol), operands = operands)

class WasmGetLocal(id: WasmLocal) :
    WasmInstruction("get_local", WasmImmediate.VariableRef(id))

class WasmSetLocal(id: WasmLocal, value: WasmInstruction) :
    WasmInstruction("set_local", WasmImmediate.VariableRef(id), listOf(value))

class WasmGetGlobal(symbol: WasmSymbol<WasmGlobal>) :
    WasmInstruction("get_global", WasmImmediate.GlobalSymbol.Linked(symbol))

class WasmSetGlobal(symbol: WasmSymbol<WasmGlobal>, value: WasmInstruction) :
    WasmInstruction("set_global", WasmImmediate.GlobalSymbol.Linked(symbol), listOf(value))

class WasmIf(condition: WasmInstruction, resultType: WasmValueType?, thenInstructions: WasmThen?, elseInstruction: WasmElse?) :
    WasmInstruction(
        "if",
        immediate = WasmImmediate.ResultType(resultType),
        operands = listOfNotNull(condition, thenInstructions, elseInstruction)
    )

class WasmBrIf(condition: WasmInstruction, target: WasmBranchTarget) :
    WasmInstruction(
        "br_if",
        immediate = WasmImmediate.BlockReference(target),
        operands = listOfNotNull(condition)
    )

sealed class WasmBranchTarget(
    val id: Int, val label: String?, mnemonic: String
) : WasmInstruction(mnemonic)

class WasmLoop(id: Int, label: String?) : WasmBranchTarget(id, label, "loop")
class WasmBlock(id: Int, label: String) : WasmBranchTarget(id, label, "block")

class WasmBr(target: WasmBranchTarget) :
    WasmInstruction(
        "br",
        immediate = WasmImmediate.BlockReference(target)
    )

class WasmThen(instructions: List<WasmInstruction>) :
    WasmInstruction("then", operands = instructions)

class WasmElse(instructions: List<WasmInstruction>) :
    WasmInstruction("else", operands = instructions)

class WasmSimpleBlock(instructions: List<WasmInstruction>, resultType: WasmValueType?) :
    WasmInstruction("block", immediate = WasmImmediate.ResultType(resultType), operands = instructions)


class WasmStructNew(structName: WasmSymbol<WasmStructType>, operands: List<WasmInstruction>) :
    WasmInstruction("struct.new", WasmImmediate.StructTypeSymbol.Linked(structName), operands)

class WasmStructSet(structName: WasmSymbol<WasmStructType>, fieldId: WasmSymbol<Int>, structRef: WasmInstruction, value: WasmInstruction) :
    WasmInstruction(
        "struct.set",
        listOf(
            WasmImmediate.StructTypeSymbol.Linked(structName),
            WasmImmediate.StructFieldSymbol.Linked(fieldId)
        ),
        listOf(structRef, value)
    )

class WasmStructGet(structName: WasmSymbol<WasmStructType>, fieldId: WasmSymbol<Int>, structRef: WasmInstruction) :
    WasmInstruction(
        "struct.get",
        listOf(
            WasmImmediate.StructTypeSymbol.Linked(structName),
            WasmImmediate.StructFieldSymbol.Linked(fieldId)
        ),
        listOf(structRef)
    )

class WasmStructNarrow(fromType: WasmValueType, toType: WasmValueType, value: WasmInstruction) :
    WasmInstruction(
        "struct.narrow", listOf(
            WasmImmediate.ValueType(fromType),
            WasmImmediate.ValueType(toType)
        ), listOf(value)
    )

sealed class WasmConst<KotlinType : Number, WasmType : WasmSimpleValueType>(value: KotlinType, type: WasmType) :
    WasmInstruction(type.mnemonic + ".const", WasmImmediate.LiteralValue<KotlinType>(value))

class WasmI32Const(value: Int) : WasmConst<Int, WasmI32>(value, WasmI32)
class WasmI64Const(value: Long) : WasmConst<Long, WasmI64>(value, WasmI64)
class WasmF32Const(value: Float) : WasmConst<Float, WasmF32>(value, WasmF32)
class WasmF64Const(value: Double) : WasmConst<Double, WasmF64>(value, WasmF64)

class WasmI32Symbol(value: WasmSymbol<Int>) :
    WasmInstruction("i32.const", WasmImmediate.I32Symbol(value))