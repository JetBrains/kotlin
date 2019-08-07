/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.wasm.ast.WasmInstruction
import org.jetbrains.kotlin.backend.wasm.ast.WasmNop
import org.jetbrains.kotlin.backend.wasm.ast.WasmSetLocal
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*

class StatementTransformer : BaseTransformer<WasmInstruction, WasmCodegenContext> {
    override fun visitVariable(declaration: IrVariable, data: WasmCodegenContext): WasmInstruction {
        val init = declaration.initializer ?: return WasmNop()
        val varName = data.getLocalName(declaration)
        return WasmSetLocal(varName, expressionToWasmInstruction(init, data))
    }

    override fun visitExpression(expression: IrExpression, data: WasmCodegenContext): WasmInstruction {
        return expressionToWasmInstruction(expression, data)
    }
}

fun statementToWasmInstruction(statement: IrStatement, context: WasmCodegenContext): WasmInstruction {
    return statement.accept(StatementTransformer(), context)
}

fun bodyToWasmInstructionList(body: IrBody, context: WasmCodegenContext): List<WasmInstruction> {
    if (body is IrBlockBody) {
        return body.statements.map { statementToWasmInstruction(it, context) }
    } else TODO()
}