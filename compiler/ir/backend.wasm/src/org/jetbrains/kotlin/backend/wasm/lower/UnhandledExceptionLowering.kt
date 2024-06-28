/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irCatch
import org.jetbrains.kotlin.backend.common.lower.irThrow
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
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.name.Name

// This pass needed to wrap around unhandled exceptions from JsExport functions and throw JS exception for call from JS site
// @JsExport
// fun someExportedMethod() {
//     error("some error message")
// }
//
// converts into
//
// @JsExport
// fun someExportedMethod() {
//     val currentIsNotFirstWasmExportCall = isNotFirstWasmExportCall
//     try {
//         isNotFirstWasmExportCall = true
//         error("some error message")
//     } catch (e: Throwable) {
//         if (currentIsNotFirstWasmExportCall) throw e else throwAsJsException(e)
//     } finally {
//         isNotFirstWasmExportCall = currentIsNotFirstWasmExportCall
//     }
// }
// TODO Wrap fieldInitializer function (now it building by later FieldInitializersLowering)

internal class UnhandledExceptionLowering(val context: WasmBackendContext) : FileLoweringPass {
    private val throwableType = context.irBuiltIns.throwableType
    private val irBooleanType = context.wasmSymbols.irBuiltIns.booleanType
    private val throwAsJsException get() = context.wasmSymbols.jsRelatedSymbols.throwAsJsException
    private val isNotFirstWasmExportCallGetter = context.wasmSymbols.isNotFirstWasmExportCall.owner.getter!!.symbol
    private val isNotFirstWasmExportCallSetter = context.wasmSymbols.isNotFirstWasmExportCall.owner.setter!!.symbol

    private fun processExportFunction(irFunction: IrFunction) {
        val body = irFunction.body ?: return
        if (body is IrBlockBody && body.statements.isEmpty()) return
        if (irFunction in context.closureCallExports.values) return

        val bodyType = when (body) {
            is IrExpressionBody -> body.expression.type
            is IrBlockBody -> context.irBuiltIns.unitType
            else -> TODO(this::class.qualifiedName!!)
        }

        with(context.createIrBuilder(irFunction.symbol)) {
            val currentIsNotFirstWasmExportCall = buildVariable(
                parent = irFunction,
                startOffset = UNDEFINED_OFFSET, endOffset = UNDEFINED_OFFSET,
                origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                name = Name.identifier("currentIsNotFirstWasmExportCall"),
                type = irBooleanType
            )

            val e = buildVariable(
                parent = irFunction,
                startOffset = UNDEFINED_OFFSET, endOffset = UNDEFINED_OFFSET,
                origin = IrDeclarationOrigin.CATCH_PARAMETER,
                name = Name.identifier("e"),
                type = throwableType
            )

            currentIsNotFirstWasmExportCall.initializer =
                irGet(irBooleanType, null, isNotFirstWasmExportCallGetter)

            val tryBody = irComposite {
                +irSet(
                    isNotFirstWasmExportCallSetter.owner.returnType,
                    null, isNotFirstWasmExportCallSetter,
                    true.toIrConst(irBooleanType)
                )
                when (body) {
                    is IrBlockBody -> body.statements.forEach { +it }
                    is IrExpressionBody -> +body.expression
                    else -> TODO(this::class.qualifiedName!!)
                }
            }

            val catch = irCatch(
                catchParameter = e,
                result = irIfThenElse(
                    type = bodyType,
                    condition = irGet(currentIsNotFirstWasmExportCall, irBooleanType),
                    thenPart = irThrow(irGet(e, throwableType)),
                    elsePart = irCall(throwAsJsException).apply { putValueArgument(0, irGet(e, throwableType)) }
                )
            )


            val finally = irSet(
                type = isNotFirstWasmExportCallSetter.owner.returnType,
                receiver = null,
                setterSymbol = isNotFirstWasmExportCallSetter,
                value = irGet(currentIsNotFirstWasmExportCall, irBooleanType)
            )

            val tryWrap = irTry(
                type = bodyType,
                tryResult = tryBody,
                catches = listOf(catch),
                finallyExpression = finally
            )

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
                else -> TODO(this::class.qualifiedName!!)
            }
        }
    }

    override fun lower(irFile: IrFile) {
        if (!context.isWasmJsTarget) return
        for (declaration in irFile.declarations) {
            if (declaration is IrFunction && declaration.isExported()) {
                processExportFunction(declaration)
            }
        }
    }
}