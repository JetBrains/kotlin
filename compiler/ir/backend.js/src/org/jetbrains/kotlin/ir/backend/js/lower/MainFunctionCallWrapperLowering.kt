/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.JsMainFunctionDetector
import org.jetbrains.kotlin.ir.backend.js.utils.compileSuspendAsJsGenerator
import org.jetbrains.kotlin.ir.backend.js.utils.isLoweredSuspendFunction
import org.jetbrains.kotlin.ir.backend.js.utils.isStringArrayParameter
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrRawFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class MainFunctionCallWrapperLowering(private val context: JsIrBackendContext) : FileLoweringPass {
    private val mainFunctionDetector = JsMainFunctionDetector(context)
    private var IrSimpleFunction.mainFunctionWrapper by context.mapping.mainFunctionToItsWrapper
    private val mainFunctionArgs by lazy(LazyThreadSafetyMode.NONE) {
        context.mainCallArguments ?: error("Expect to have main call args at this point")
    }

    override fun lower(irFile: IrFile) {
        if (context.mainCallArguments == null) return
        val mainFunction = mainFunctionDetector.getMainFunctionOrNull(irFile) ?: return
        val mainFunctionWrapper = context.irFactory.stageController.restrictTo(mainFunction) {
            mainFunction.generateWrapperForMainFunction().also {
                mainFunction.mainFunctionWrapper = it
            }
        }

        irFile.declarations.add(mainFunctionWrapper)
    }

    private fun IrSimpleFunction.generateWrapperForMainFunction(): IrSimpleFunction {
        val originalFunctionSymbol = symbol
        return context.irFactory.createSimpleFunction(
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
        ).also {
            it.parent = parent
            it.body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET).apply {
                val shouldCallMainFunctionAsCoroutine = isLoweredSuspendFunction(context) && context.compileSuspendAsJsGenerator
                val functionSymbolToCall = when {
                    !shouldCallMainFunctionAsCoroutine -> originalFunctionSymbol
                    hasStringArrayParameter() -> context.intrinsics.startCoroutineUninterceptedOrReturnGeneratorVersion2
                    else -> context.intrinsics.startCoroutineUninterceptedOrReturnGeneratorVersion1
                }

                val mainFunctionCall = JsIrBuilder.buildCall(functionSymbolToCall).apply {
                    if (shouldCallMainFunctionAsCoroutine) {
                        extensionReceiver = IrRawFunctionReferenceImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            context.irBuiltIns.anyType,
                            originalFunctionSymbol
                        )
                    }
                    generateMainArguments().forEachIndexed { index, arg ->
                        putValueArgument(index, arg)
                    }
                }

                statements.add(mainFunctionCall)
            }
        }
    }

    private fun IrSimpleFunction.generateMainArguments(): List<IrExpression> {
        return listOfNotNull(
            runIf(hasStringArrayParameter()) {
                context.platformArgumentsProviderJsExpression?.let {
                    JsIrBuilder.buildCall(context.intrinsics.jsCode).apply {
                        putValueArgument(0, it.toIrConst(context.irBuiltIns.stringType))
                    }
                } ?: JsIrBuilder.buildArray(
                    mainFunctionArgs.map { it.toIrConst(context.irBuiltIns.stringType) },
                    valueParameters.first().type,
                    context.irBuiltIns.stringType
                )
            },
            runIf(isLoweredSuspendFunction(context)) {
                JsIrBuilder.buildCall(context.coroutineEmptyContinuation.owner.getter!!.symbol)
            }
        )
    }

    private fun IrSimpleFunction.hasStringArrayParameter(): Boolean {
        return valueParameters.firstOrNull()?.isStringArrayParameter() == true
    }
}