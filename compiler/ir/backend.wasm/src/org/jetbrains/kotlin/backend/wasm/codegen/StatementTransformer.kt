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

class StatementTransformer : BaseTransformer<List<WasmInstruction>, WasmFunctionCodegenContext> {
    override fun visitVariable(declaration: IrVariable, data: WasmFunctionCodegenContext): List<WasmInstruction> {
        data.defineLocal(declaration.symbol)
        val init = declaration.initializer ?: return emptyList()
        val varName = data.referenceLocal(declaration.symbol)
        return listOf(WasmSetLocal(varName, expressionToWasmInstruction(init, data)))
    }

    override fun visitExpression(expression: IrExpression, data: WasmFunctionCodegenContext): List<WasmInstruction> {
        return listOf(expressionToWasmInstruction(expression, data))
    }

    override fun visitWhen(expression: IrWhen, data: WasmFunctionCodegenContext): List<WasmInstruction> {
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

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: WasmFunctionCodegenContext): List<WasmInstruction> {
        // (loop $LABEL
        //     (block $BREAK_LABEL
        //         (block $CONTINUE_LABEL <LOOP BODY>)
        //         (br_if $LABEL          <CONDITION>)))

        val label = loop.label
        val wasmLoop = WasmLoop(data.getNextLabelId(), label)
        val wasmBreakBlock = WasmBlock(data.getNextLabelId(),"BREAK_$label")
        val wasmContinueBlock = WasmBlock(data.getNextLabelId(), "CONTINUE_$label")
        data.defineLoopLabel(loop, LoopLabelType.BREAK, wasmBreakBlock)
        data.defineLoopLabel(loop, LoopLabelType.CONTINUE, wasmContinueBlock)

        val wasmLoopBody = loop.body?.let { statementToWasmInstruction(it, data) }.orEmpty()
        val wasmCondition = expressionToWasmInstruction(loop.condition, data)
        val wasmCheckAndContinue = WasmBrIf(wasmCondition, wasmLoop)

        wasmContinueBlock.blockBody += wasmLoopBody
        wasmBreakBlock.blockBody += wasmContinueBlock
        wasmBreakBlock.blockBody += wasmCheckAndContinue
        wasmLoop.blockBody += wasmBreakBlock

        return listOf(wasmLoop)
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: WasmFunctionCodegenContext): List<WasmInstruction> {
        // (loop $CONTINUE_LABEL
        //     (block $BREAK_LABEL
        //         (br_if $BREAK_LABEL (i32.eqz <CONDITION>))
        //         <LOOP_BODY>
        //         (br $CONTINUE_LABEL)))

        val label = loop.label
        val wasmLoop = WasmLoop(data.getNextLabelId(), label)
        val wasmBreakBlock = WasmBlock(data.getNextLabelId(),"BREAK_$label")
        data.defineLoopLabel(loop, LoopLabelType.BREAK, wasmBreakBlock)
        data.defineLoopLabel(loop, LoopLabelType.CONTINUE, wasmLoop)

        val wasmLoopBody = loop.body?.let { statementToWasmInstruction(it, data) }.orEmpty()
        val wasmCondition = expressionToWasmInstruction(loop.condition, data)
        val wasmCheckAndBreak = WasmBrIf(WasmNot(wasmCondition), wasmBreakBlock)

        wasmBreakBlock.blockBody += wasmCheckAndBreak
        wasmBreakBlock.blockBody += wasmLoopBody
        wasmBreakBlock.blockBody += WasmBr(wasmLoop)

        wasmLoop.blockBody += wasmBreakBlock

        return listOf(wasmLoop)
    }
}

fun statementToWasmInstruction(statement: IrStatement, context: WasmFunctionCodegenContext): List<WasmInstruction> {
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