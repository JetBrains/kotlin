/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.kotlin.backend.wasm.ast

class WasmMemoryArgument(val align: Int, val offset: Int)

sealed class WasmInstruction {
    abstract val operator: WasmOp
    open val type: WasmValueType? = null
    abstract fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R
}

sealed class WasmTypedOpInstruction : WasmInstruction() {
    abstract override val operator: WasmTypedOp
    override val type: WasmValueType?
        get() = operator.type
}

class WasmUnary(
    override val operator: WasmUnaryOp,
    val operand: WasmInstruction
) : WasmTypedOpInstruction() {
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitUnary(this, data)
}

class WasmBinary(
    override val operator: WasmBinaryOp,
    val lhs: WasmInstruction,
    val rhs: WasmInstruction
) : WasmTypedOpInstruction() {
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitBinary(this, data)
}

sealed class WasmConstant(
    override val operator: WasmConstantOp
) : WasmTypedOpInstruction() {
    class I32(val value: Int) : WasmConstant(WasmConstantOp.I32_CONST)
    class I64(val value: Long) : WasmConstant(WasmConstantOp.I64_CONST)
    class F32(val value: Float) : WasmConstant(WasmConstantOp.F32_CONST)
    class F64(val value: Double) : WasmConstant(WasmConstantOp.F64_CONST)
    object F32NaN : WasmConstant(WasmConstantOp.F32_CONST)
    object F64NaN : WasmConstant(WasmConstantOp.F64_CONST)

    class I32Symbol(val value: WasmSymbol<Int>) : WasmConstant(WasmConstantOp.I32_CONST)

    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitConstant(this, data)
}

class WasmLoad(
    override val operator: WasmLoadOp,
    val memoryArgument: WasmMemoryArgument,
    val address: WasmInstruction
) : WasmTypedOpInstruction() {
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitLoad(this, data)
}

class WasmStore(
    override val operator: WasmStoreOp,
    val memoryArgument: WasmMemoryArgument,
    val address: WasmInstruction,
    val value: WasmInstruction
) : WasmTypedOpInstruction() {
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitStore(this, data)
}

sealed class WasmControl(
    override val operator: WasmControlOp
) : WasmInstruction()

sealed class WasmParametric(
    override val operator: WasmParametricOp
) : WasmInstruction()

sealed class WasmVariable(
    override val operator: WasmVariableOp
) : WasmInstruction()

object WasmUnreachable : WasmControl(WasmControlOp.UNREACHABLE) {
    override val type = WasmUnreachableType
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitUnreachable(this, data)
}

object WasmNop : WasmControl(WasmControlOp.NOP) {
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitNop(this, data)
}

sealed class WasmBranchTarget(
    val id: Int?,
    val label: String?,
    override val operator: WasmControlOp
) : WasmInstruction()

class WasmBlock(
    id: Int?,
    label: String?,
    override val type: WasmValueType? = null,
    val instructions: MutableList<WasmInstruction> = mutableListOf()
) : WasmBranchTarget(id, label, WasmControlOp.BLOCK) {
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitBlock(this, data)
}

class WasmLoop(
    id: Int?,
    label: String?,
    override val type: WasmValueType? = null,
    val instructions: MutableList<WasmInstruction> = mutableListOf()
) : WasmBranchTarget(id, label, WasmControlOp.LOOP) {
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitLoop(this, data)
}

class WasmIf(
    override val type: WasmValueType?,
    val condition: WasmInstruction,
    val thenBlock: List<WasmInstruction>,
    val elseBlock: List<WasmInstruction>
) : WasmControl(WasmControlOp.IF) {
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitIf(this, data)
}

class WasmBr(val target: WasmBranchTarget) :
    WasmControl(WasmControlOp.BR) {
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitBr(this, data)
}

class WasmBrIf(
    val condition: WasmInstruction,
    val target: WasmBranchTarget
) : WasmControl(WasmControlOp.BR_IF) {
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitBrIf(this, data)
}

class WasmBrTable(
    val index: WasmInstruction,
    val targets: List<WasmBranchTarget>,
    val defaultTarget: WasmBranchTarget
) : WasmControl(WasmControlOp.BR_TABLE) {
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitBrTable(this, data)
}

class WasmReturn(
    val value: WasmInstruction?
) : WasmControl(WasmControlOp.RETURN) {
    override val type = WasmUnreachableType
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitReturn(this, data)
}

class WasmCall(
    val symbol: WasmSymbol<WasmFunction>,
    val arguments: List<WasmInstruction>
) : WasmControl(WasmControlOp.CALL) {
    override val type
        get() = symbol.owner.type.resultType

    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitCall(this, data)
}

class WasmCallIndirect(
    val symbol: WasmSymbol<WasmFunctionType>,
    val arguments: List<WasmInstruction>,
    val functionIdx: WasmInstruction
) : WasmControl(WasmControlOp.CALL_INDIRECT) {
    override val type
        get() = symbol.owner.resultType

    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitCallIndirect(this, data)
}

class WasmDrop(
    val value: WasmInstruction
) : WasmParametric(WasmParametricOp.DROP) {
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitDrop(this, data)
}

class WasmSelect(
    val operand1: WasmInstruction,
    val operand2: WasmInstruction,
    val condition: WasmInstruction
) : WasmParametric(WasmParametricOp.SELECT) {
    override val type = operand1.type
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitSelect(this, data)
}

class WasmGetLocal(
    val local: WasmLocal
) : WasmVariable(WasmVariableOp.LOCAL_GET) {
    override val type = local.type
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitGetLocal(this, data)
}

class WasmSetLocal(
    val local: WasmLocal,
    val value: WasmInstruction
) : WasmVariable(WasmVariableOp.LOCAL_SET) {
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitSetLocal(this, data)
}

class WasmLocalTee(
    val local: WasmLocal,
    val value: WasmInstruction
) : WasmVariable(WasmVariableOp.LOCAL_TEE) {
    override val type = local.type
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitLocalTee(this, data)
}

class WasmGetGlobal(
    val global: WasmSymbol<WasmGlobal>,
    override val type: WasmValueType
) : WasmVariable(WasmVariableOp.GLOBAL_GET) {
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitGetGlobal(this, data)
}

class WasmSetGlobal(
    val global: WasmSymbol<WasmGlobal>,
    val value: WasmInstruction
) : WasmVariable(WasmVariableOp.GLOBAL_SET) {
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitSetGlobal(this, data)
}


sealed class WasmStructBased(
    override val operator: WasmStructOp
) : WasmInstruction()


class WasmStructGet(
    val structName: WasmSymbol<WasmStructType>,
    val fieldId: WasmSymbol<Int>,
    val structRef: WasmInstruction,
    override val type: WasmValueType
) : WasmStructBased(WasmStructOp.STRUCT_GET) {
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitStructGet(this, data)
}

class WasmStructNew(
    val structName: WasmSymbol<WasmStructType>,
    val operands: List<WasmInstruction>
) : WasmStructBased(WasmStructOp.STRUCT_NEW) {
    override val type
        get() = WasmStructRef(structName)

    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitStructNew(this, data)
}

class WasmStructSet(
    val structName: WasmSymbol<WasmStructType>,
    val fieldId: WasmSymbol<Int>,
    val structRef: WasmInstruction,
    val value: WasmInstruction
) : WasmStructBased(WasmStructOp.STRUCT_SET) {
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitStructSet(this, data)
}

class WasmStructNarrow(
    val fromType: WasmValueType,
    override val type: WasmValueType,
    val value: WasmInstruction
) : WasmStructBased(WasmStructOp.STRUCT_NARROW) {
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitStructNarrow(this, data)
}

sealed class WasmRefBased(
    override val operator: WasmRefOp
) : WasmInstruction()

object WasmRefNull : WasmRefBased(WasmRefOp.REF_NULL) {
    override val type = WasmNullRefType
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitRefNull(this, data)
}

class WasmRefIsNull(
    val value: WasmInstruction
) : WasmRefBased(WasmRefOp.REF_IS_NULL) {
    override val type = WasmI32
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitRefIsNull(this, data)
}

class WasmRefEq(
    val lhs: WasmInstruction,
    val rhs: WasmInstruction
) : WasmRefBased(WasmRefOp.REF_EQ) {
    override val type = WasmI32
    override fun <R, D> accept(visitor: WasmExpressionVisitor<R, D>, data: D): R =
        visitor.visitRefEq(this, data)
}

// ---------------------
// Helpers

fun wasmNot(boolValue: WasmInstruction) = WasmUnary(WasmUnaryOp.I32_EQZ, boolValue)

