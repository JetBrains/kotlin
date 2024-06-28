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
import org.jetbrains.kotlin.ir.backend.js.utils.JsMainFunctionDetector
import org.jetbrains.kotlin.ir.backend.js.utils.isLoweredSuspendFunction
import org.jetbrains.kotlin.ir.backend.js.utils.isStringArrayParameter
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression

/**
 * Find single most appropriate main function and call with empty arguments from
 * [WasmBackendContext.mainCallsWrapperFunction].
 */
class GenerateMainFunctionCalls(private val backendContext: WasmBackendContext) : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        val mainFunction = JsMainFunctionDetector(backendContext).getMainFunctionOrNull(irModule) ?: return
        val generateArgv = mainFunction.valueParameters.firstOrNull()?.isStringArrayParameter() ?: false
        val generateContinuation = mainFunction.isLoweredSuspendFunction(backendContext)
        with(backendContext.createIrBuilder(backendContext.mainCallsWrapperFunction.symbol)) {
            val argv = if (generateArgv) {
                backendContext.createArrayOfExpression(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.stringType, emptyList())
            } else {
                null
            }

            val continuation =
                if (generateContinuation) {
                    irCall(backendContext.wasmSymbols.coroutineEmptyContinuation.owner.getter!!)
                } else {
                    null
                }

            (backendContext.mainCallsWrapperFunction.body as IrBlockBody).statements += irCall(mainFunction).also { call ->
                listOfNotNull(argv, continuation).forEachIndexed { index: Int, arg: IrExpression -> call.putValueArgument(index, arg) }
            }
        }
    }
}
