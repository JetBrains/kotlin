/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.kotlin.backend.wasm.ast.instructions

import org.jetbrains.kotlin.backend.wasm.ast.*

// -------------------------

class WasmMemoryArgument

// -------------------------

sealed class WasmExpression {
    abstract val operator: WasmOp
}

class WasmUnaryExpression(
    override val operator: WasmUnaryOp,
    val operand: WasmExpression
) : WasmExpression()

class WasmBinaryExpression(
    override val operator: WasmBinaryOp,
    val lhs: WasmExpression,
    val rhs: WasmExpression
) : WasmExpression()

sealed class WasmConstant(
    override val operator: WasmConstantOp
) : WasmExpression() {
    class I32(val value: Int) : WasmConstant(WasmConstantOp.I32_CONST)
    class I64(val value: Long) : WasmConstant(WasmConstantOp.I64_CONST)
    class F32(val value: Float) : WasmConstant(WasmConstantOp.F32_CONST)
    class F64(val value: Double) : WasmConstant(WasmConstantOp.F64_CONST)
}

class WasmLoadExpression(
    override val operator: WasmLoadOp,
    val memoryArgument: WasmMemoryArgument,
    val address: WasmExpression
) : WasmExpression()

class WasmStoreExpression(
    override val operator: WasmStoreOp,
    val memoryArgument: WasmMemoryArgument,
    val address: WasmExpression,
    val value: WasmExpression
) : WasmExpression()

sealed class WasmControlExpression(
    override val operator: WasmControlOp
) : WasmExpression()

object WasmUnreachable : WasmControlExpression(WasmControlOp.UNREACHABLE)
object WasmNop : WasmControlExpression(WasmControlOp.NOP)

sealed class WasmBranchTarget(
    val label: String?,
    override val operator: WasmControlOp
) : WasmExpression()

class WasmBlockExpression(
    val resultType: WasmValueType?,
    val instructions: List<WasmInstruction>,
    label: String?
) : WasmBranchTarget(label, WasmControlOp.BLOCK)

class WasmLoopExpression(
    val resultType: WasmValueType?,
    val instructions: List<WasmInstruction>,
    label: String?
) : WasmBranchTarget(label, WasmControlOp.LOOP)

class WasmIfExpression(
    val resultType: WasmValueType?,
    val condition: WasmInstruction,
    val thenBlock: List<WasmInstruction>,
    val elseBlock: List<WasmInstruction>
) : WasmControlExpression(WasmControlOp.IF)

class WasmBrExpression(val target: WasmBranchTarget) :
    WasmControlExpression(WasmControlOp.BR)