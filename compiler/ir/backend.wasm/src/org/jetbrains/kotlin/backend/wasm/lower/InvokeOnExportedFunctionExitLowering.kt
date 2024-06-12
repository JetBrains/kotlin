/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.syntheticBodyIsNotSupported
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.ir2wasm.isExported
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.name.Name

// This pass needed to call coroutines event loop run after exported functions calls
// @WasmExport
// fun someExportedMethod() {
//     println("hello world")
// }
//
// converts into
//
// @WasmExport
// fun someExportedMethod() {
//     val currentIsNotFirstWasmExportCall = isNotFirstWasmExportCall
//     try {
//         isNotFirstWasmExportCall = true
//         println("hello world")
//     } finally {
//         isNotFirstWasmExportCall = currentIsNotFirstWasmExportCall
//         if (!currentIsNotFirstWasmExportCall) invokeOnExportedFunctionExit()
//     }
// }

internal class InvokeOnExportedFunctionExitLowering(val context: WasmBackendContext) : FileLoweringPass {
    private val invokeOnExportedFunctionExit get() = context.wasmSymbols.invokeOnExportedFunctionExit
    private val irBooleanType = context.wasmSymbols.irBuiltIns.booleanType
    private val isNotFirstWasmExportCallGetter = context.wasmSymbols.isNotFirstWasmExportCall.owner.getter!!.symbol
    private val isNotFirstWasmExportCallSetter = context.wasmSymbols.isNotFirstWasmExportCall.owner.setter!!.symbol

    private fun processExportFunction(irFunction: IrFunction) {
        val body = irFunction.body ?: return
        if (body is IrBlockBody && body.statements.isEmpty()) return
        context.applyIfDefined(irFunction.file) {
            if (irFunction in it.closureCallExports.values) return
        }

        val bodyType = when (body) {
            is IrExpressionBody -> body.expression.type
            is IrBlockBody -> context.irBuiltIns.unitType
            is IrSyntheticBody -> syntheticBodyIsNotSupported(irFunction)
        }

        with(context.createIrBuilder(irFunction.symbol)) {
            val currentIsNotFirstWasmExportCall = buildVariable(
                parent = irFunction,
                startOffset = UNDEFINED_OFFSET, endOffset = UNDEFINED_OFFSET,
                origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                name = Name.identifier("currentIsNotFirstWasmExportCall"),
                type = irBooleanType
            )

            currentIsNotFirstWasmExportCall.initializer =
                irGet(irBooleanType, null, isNotFirstWasmExportCallGetter)

            val tryBody = irComposite {
                +irSet(
                    isNotFirstWasmExportCallSetter.owner.returnType,
                    null, isNotFirstWasmExportCallSetter,
                    true.toIrConst(irBooleanType)
                )

                +body.statements
            }

            val finally = irComposite(resultType = context.irBuiltIns.unitType) {
                +irSet(
                    type = isNotFirstWasmExportCallSetter.owner.returnType,
                    receiver = null,
                    setterSymbol = isNotFirstWasmExportCallSetter,
                    value = irGet(currentIsNotFirstWasmExportCall, irBooleanType)
                )
                +irIfThen(
                    type = context.irBuiltIns.unitType,
                    condition = irNot(irGet(currentIsNotFirstWasmExportCall, irBooleanType)),
                    thenPart = irCall(invokeOnExportedFunctionExit),
                )
            }

            val tryWrap = irTry(
                type = bodyType,
                tryResult = tryBody,
                catches = emptyList(),
                finallyExpression = finally
            )

            @Suppress("KotlinConstantConditions")
            when (body) {
                is IrExpressionBody -> body.expression = irComposite {
                    +currentIsNotFirstWasmExportCall
                    +tryWrap
                }
                is IrBlockBody -> with(body.statements) {
                    clear()
                    add(currentIsNotFirstWasmExportCall)
                    add(tryWrap)
                }
                is IrSyntheticBody -> syntheticBodyIsNotSupported(irFunction)
            }
        }
    }

    override fun lower(irFile: IrFile) {
        if (context.isWasmJsTarget) return
        for (declaration in irFile.declarations) {
            if (declaration is IrFunction && (declaration.isExported() || declaration == context.getFileContext(irFile).mainFunctionWrapper)) {
                processExportFunction(declaration)
            }
        }
    }
}