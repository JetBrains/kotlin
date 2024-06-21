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
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.name.Name

/**
 * Find single most appropriate main function and call with empty arguments and generate wrappers for not simple one's
 */
class GenerateMainFunctionWrappers(private val backendContext: WasmBackendContext) : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        val mainFunction = JsMainFunctionDetector(backendContext).getMainFunctionOrNull(irModule) ?: return
        val generateArgv = mainFunction.valueParameters.firstOrNull()?.isStringArrayParameter() ?: false
        val generateContinuation = mainFunction.isLoweredSuspendFunction(backendContext)

        if (!generateArgv && !generateContinuation) {
            backendContext.getFileContext(mainFunction.file).mainFunctionWrapper = mainFunction
            return
        }

        val wrapper = backendContext.irFactory.stageController.restrictTo(mainFunction) {
            mainFunction.createMainFunctionWrapper(backendContext, generateArgv, generateContinuation)
        }
        backendContext.getFileContext(mainFunction.file).mainFunctionWrapper = wrapper
    }
}

private fun IrSimpleFunction.createMainFunctionWrapper(
    backendContext: WasmBackendContext,
    generateArgv: Boolean,
    generateContinuation: Boolean
): IrSimpleFunction {
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

    with(backendContext.createIrBuilder(this.symbol)) {
        val argv = if (generateArgv) {
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
            listOfNotNull(argv, continuation).forEachIndexed { index: Int, arg: IrExpression -> call.putValueArgument(index, arg) }
        }

        wrapperBody.statements += irReturn(call)
        mainWrapper.body = wrapperBody
    }

    return mainWrapper
}

