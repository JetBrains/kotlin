/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.wasm.ast.WasmInstruction
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.IrNamer
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression

fun bodyToWasm(body: IrBody, namer: IrNamer): List<WasmInstruction> {
    if (body is IrBlockBody) {
        return body.statements.map { statementToWasmInstruction(it, namer) }
    }
    else TODO()
}

fun statementToWasmInstruction(statement: IrStatement, namer: IrNamer): WasmInstruction {
    return statement.accept(IrElementToWasmStatementTransformer(), namer)
}

fun expressionToWasmInstruction(expression: IrExpression, namer: IrNamer): WasmInstruction {
    return expression.accept(IrElementToWasmExpressionTransformer(), namer)
}