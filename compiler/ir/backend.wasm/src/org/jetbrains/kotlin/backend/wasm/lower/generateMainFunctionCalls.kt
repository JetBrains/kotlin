/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.ir.createArrayOfExpression
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irCatch
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.JsMainFunctionDetector
import org.jetbrains.kotlin.ir.backend.js.utils.isLoweredSuspendFunction
import org.jetbrains.kotlin.ir.backend.js.utils.isStringArrayParameter
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irTry
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.validation.IrValidatorConfig
import org.jetbrains.kotlin.ir.validation.validateIr
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.wasm.config.wasmTarget
import kotlin.system.exitProcess

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
            val isWasiPreview2 = backendContext.configuration.wasmTarget == WasmTarget.WASI
            // if we're on wasi preview 2, we always need to generate a wrapper, to have a function to export that adheres to the correct ABI

            val fileContext = backendContext.getFileContext(file)


            if (!generateArgv && !generateContinuation && !isWasiPreview2) {
                fileContext.mainFunctionWrapper = mainFunction
                continue
            }

            val wrapper = backendContext.irFactory.stageController.restrictTo(mainFunction) {
                mainFunction.createMainFunctionWrapper(
                    backendContext,
                    generateArgv,
                    generateContinuation,
                    isWasiPreview2
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
    isWasiPreview2: Boolean,
): IrSimpleFunction {
    val returnType = if (!isWasiPreview2)
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
            if (!isWasiPreview2) {
                backendContext.createArrayOfExpression(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    context.irBuiltIns.stringType,
                    emptyList()
                )
            } else {
                // TODO Need to put wasi:cli/environment in stdlib - but should it generally accessible?
                backendContext.createArrayOfExpression(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    context.irBuiltIns.stringType,
                    emptyList()
                )
            }
        } else {
            null
        }

        val continuation =
            if (generateContinuation) {
                irCall(backendContext.wasmSymbols.coroutineEmptyContinuation.owner.getter!!)
            } else {
                null
            }

        // for wasi, uncaught exceptions leaking into the environment are a problem, so surround the call with a try catch, that also determines the exit code

        val wrapperBody = backendContext.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET)

        // TODO if we use the wasi preview 1 adapter, need to freeAllComponentModelReallocAllocatedMemory() at the start of this as well
        //   - the adapter itself is only needed if we have custom wasi preview 1 programs that need auto-adaption
        //   - problem is, that it uses realloc to do general purpose mem alloc, which is what leads to KT-86415

        val call = irCall(this@createMainFunctionWrapper).also { call ->
            listOfNotNull(argv, continuation).forEachIndexed { index: Int, arg: IrExpression -> call.arguments[index] = arg }
        }

        if (!isWasiPreview2)
            wrapperBody.statements += irReturn(call)
        else {
            wrapperBody.statements += irBlock {
                val catchParameterThrowable = this@irBlock.scope.createTemporaryVariableDeclaration(
                    context.irBuiltIns.throwableType,
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET
                )

                // TODO see if this works to pass it to the catch block
                val catchBlock = irBlock {
                    // TODO ============ reconsider this ============
                    +irCall(backendContext.wasmSymbols.printStackTrace).apply {
                        arguments[0] = irGet(catchParameterThrowable)
                    }
                    // TODO ============ end reconsider this ============
                    +irReturn(irInt(1))
                }

                +irTry(
                    context.irBuiltIns.unitType,
                    tryResult = irBlock {
                        +call
                        +irReturn(irInt(0))
                    },
                    catches = listOf(irCatch(catchParameterThrowable, catchBlock)),
                    finallyExpression = null
                )
            }
        }
        mainWrapper.body = wrapperBody
    }

    // TODO remove, is test only
    validateIr(
        mainWrapper, backendContext.irBuiltIns, IrValidatorConfig(true, true),
        {err ->
            println(err)
            exitProcess(1)
        }
    )

    return mainWrapper
}
