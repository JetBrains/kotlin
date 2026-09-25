/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.ir.createArrayOfExpression
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.JsMainFunctionDetector
import org.jetbrains.kotlin.ir.backend.js.utils.isLoweredSuspendFunction
import org.jetbrains.kotlin.ir.backend.js.utils.isStringArrayParameter
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.wasm.config.wasmTarget

/**
 * Find single most appropriate main function and call with empty arguments and generate wrappers for not simple one's
 */
class GenerateMainFunctionWrappers(private val backendContext: WasmBackendContext) : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        if (backendContext.irModuleFragment != irModule) return

        val detector = JsMainFunctionDetector(backendContext)
        for (file in irModule.files) {
            val mainFunction = detector.getMainFunctionOrNull(file) ?: continue
            val generateArgv = mainFunction.parameters.firstOrNull()?.isStringArrayParameter() ?: false
            val generateContinuation = mainFunction.isLoweredSuspendFunction(backendContext)
            val isWasi = backendContext.configuration.wasmTarget == WasmTarget.WASI

            val fileContext = backendContext.getFileContext(file)


            // in the case of WASI, we always need to generate a wrapper, in order to have a function to export that adheres to the canonical ABI
            if (!generateArgv && !generateContinuation && !isWasi) {
                fileContext.mainFunctionWrapper = mainFunction
                continue
            }

            val wrapper = backendContext.irFactory.stageController.restrictTo(mainFunction) {
                mainFunction.createMainFunctionWrapper(
                    backendContext,
                    generateArgv,
                    generateContinuation,
                    isWasi
                )
            }
            fileContext.mainFunctionWrapper = wrapper
        }
    }
}

private fun IrSimpleFunction.createMainFunctionWrapper(
    backendContext: WasmBackendContext,
    generateArgv: Boolean,
    generateContinuation: Boolean,
    isWasi: Boolean,
): IrSimpleFunction {
    // NOTE: the key difference that WASI makes, is that it's main function (wasi:cli/run) needs to return an integer
    val returnType = if (!isWasi)
        backendContext.irBuiltIns.unitType
    else
        backendContext.irBuiltIns.intType

    val mainWrapper = backendContext.irFactory.createSimpleFunction(
        startOffset = UNDEFINED_OFFSET,
        endOffset = UNDEFINED_OFFSET,
        origin = JsIrBuilder.SYNTHESIZED_DECLARATION,
        name = Name.identifier("mainWrapper"),
        visibility = visibility,
        isInline = false,
        isExpect = false,
        returnType = returnType,
        modality = modality,
        symbol = IrSimpleFunctionSymbolImpl(),
        isTailrec = false,
        isSuspend = false,
        isOperator = false,
        isInfix = false
    )

    mainWrapper.parent = file
    file.declarations.add(mainWrapper)

    with(backendContext.createIrBuilder(mainWrapper.symbol)) {
        val argv = if (generateArgv) {
            // TODO(KT-89278): support commandline arguments via wasi:cli/environment
            backendContext.createArrayOfExpression(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                context.irBuiltIns.stringType,
                emptyList()
            )
        } else {
            null
        }

        val continuation =
            if (generateContinuation) {
                irCall(backendContext.wasmSymbols.coroutineEmptyContinuation.owner.getter!!)
            } else {
                null
            }

        val wrapperBody = backendContext.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET)

        val call = irCall(this@createMainFunctionWrapper).also { call ->
            listOfNotNull(argv, continuation).forEachIndexed { index: Int, arg: IrExpression -> call.arguments[index] = arg }
        }

        if (!isWasi) {
            wrapperBody.statements += irReturn(call)
        } else {
            wrapperBody.statements += irBlock {
                // NOTE: we explicitly do NOT wrap this call in a try-catch, as we want to let uncaught exceptions leak into the environment
                +call
                +irReturn(irInt(0))
            }
        }
        mainWrapper.body = wrapperBody
    }

    return mainWrapper
}
