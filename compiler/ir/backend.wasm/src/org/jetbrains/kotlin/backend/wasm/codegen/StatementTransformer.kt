/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.common.ir.isElseBranch
import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*

class StatementTransformer : BaseTransformer<List<WasmInstruction>, WasmCodegenContext> {
    override fun visitVariable(declaration: IrVariable, data: WasmCodegenContext): List<WasmInstruction> {
        val init = declaration.initializer ?: return emptyList()
        val varName = data.getLocalName(declaration)
        return listOf(WasmSetLocal(varName, expressionToWasmInstruction(init, data)))
    }

    override fun visitExpression(expression: IrExpression, data: WasmCodegenContext): List<WasmInstruction> {
        return listOf(expressionToWasmInstruction(expression, data))
    }

    override fun visitWhen(expression: IrWhen, data: WasmCodegenContext): List<WasmInstruction> {
        return expression.branches.foldRight(emptyList()) { br: IrBranch, insts: List<WasmInstruction> ->
            val body = statementToWasmInstruction(br.result, data)
            if (isElseBranch(br)) body
            else {
                val condition = expressionToWasmInstruction(br.condition, data)
                val wasmElse = if (insts.isNotEmpty()) WasmElse(insts) else null
                listOf(WasmIf(condition, null, WasmThen(body), wasmElse))
            }
        }
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: WasmCodegenContext): List<WasmInstruction> {
        val breakLabel = data.getBreakLabelName(loop)
        val continueLabel = data.getContinueLabelName(loop)
        val loopLabel = data.getLoopLabelName(loop)

        val wasmLoopBody = loop.body?.let { statementToWasmInstruction(it, data) } ?: emptyList()
        val wasmCondition = expressionToWasmInstruction(loop.condition, data)
        val wasmCheckAndContinue = WasmBrIf(wasmCondition, loopLabel)

        val bodyWithExitCondition = listOf(WasmLabelledBlock(continueLabel, wasmLoopBody), wasmCheckAndContinue)
        val wasmLoop = WasmLoop(loopLabel, listOf(WasmLabelledBlock(breakLabel, bodyWithExitCondition)))
        return listOf(wasmLoop)
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: WasmCodegenContext): List<WasmInstruction> {
        val breakLabel = data.getBreakLabelName(loop)
        val continueLabel = data.getContinueLabelName(loop)

        val wasmLoopBody = loop.body?.let { statementToWasmInstruction(it, data) } ?: emptyList()
        val wasmCondition = expressionToWasmInstruction(loop.condition, data)
        val wasmCheckAndContinue = WasmBrIf(WasmNot(wasmCondition), breakLabel)

        val bodyWithExitCondition = listOf(wasmCheckAndContinue) + wasmLoopBody + listOf(WasmBr(continueLabel))
        val wasmLoop = WasmLoop(continueLabel, listOf(WasmLabelledBlock(breakLabel, bodyWithExitCondition)))
        return listOf(wasmLoop)
    }
}

fun statementToWasmInstruction(statement: IrStatement, context: WasmCodegenContext): List<WasmInstruction> {
    if (statement is IrContainerExpression)
        return statement.statements.flatMap { statementToWasmInstruction(it, context) }

    val instructions = statement.accept(StatementTransformer(), context)
    val ib = context.backendContext.irBuiltIns
    if (statement is IrExpression) {

        // No need to mark returns as unreachable
        if (statement is IrReturn)
            return instructions

        return when (statement.type) {
            ib.nothingType -> instructions + listOf(WasmUnreachable)
            ib.unitType -> instructions
            else -> instructions + listOf(WasmDrop(emptyList()))
        }
    }
    return instructions
}

fun bodyToWasmInstructionList(body: IrBody, context: WasmCodegenContext): List<WasmInstruction> {
    if (body is IrBlockBody) {
        return body.statements.flatMap { statementToWasmInstruction(it, context) }
    } else {
        error("Expression bodies must be lowered")
    }
}