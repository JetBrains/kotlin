/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.previousOffset
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

class JsSuspendFunctionWithGeneratorsLowering(private val context: JsIrBackendContext) : BodyLoweringPass {
    private val getContinuationSymbol = context.ir.symbols.getContinuation
    private val jsYieldFunctionSymbol = context.intrinsics.jsYieldFunctionSymbol
    private val suspendOrReturnFunctionSymbol = context.intrinsics.suspendOrReturnFunctionSymbol
    private val coroutineSuspendedGetterSymbol = context.coroutineSymbols.coroutineSuspendedGetter

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container is IrSimpleFunction && container.isSuspend) {
            transformSuspendFunction(container, irBody)
        }
    }

    private fun transformSuspendFunction(function: IrSimpleFunction, body: IrBody): IrFunction {
        return function.apply {
            when (val functionKind = getSuspendFunctionKind(context, this, body, includeSuspendLambda = false)) {
                is SuspendFunctionKind.NO_SUSPEND_CALLS -> {}
                is SuspendFunctionKind.DELEGATING -> {
                    removeReturnIfSuspendedCallAndSimplifyDelegatingCall(this, functionKind.delegatingCall)
                }
                is SuspendFunctionKind.NEEDS_STATE_MACHINE -> {
                    addJsGeneratorAnnotation(function)
                    putYieldIntrinsicCallInAllSuspensionPoints(body)
                }
            }
        }
    }

    private fun addJsGeneratorAnnotation(function: IrSimpleFunction) {
        function.annotations = function.annotations memoryOptimizedPlus JsIrBuilder.buildConstructorCall(
            context.intrinsics.jsGeneratorAnnotationSymbol.owner.primaryConstructor!!.symbol
        )
    }

    private fun putYieldIntrinsicCallInAllSuspensionPoints(body: IrBody) {
        body.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val call = super.visitCall(expression)
                return if (call !is IrCall || !call.symbol.owner.isSuspend) {
                    call
                } else {
                    context.createIrBuilder(call.symbol).run {
                        irBlock(resultType = call.type) {
                            val suspendOrReturnCall = irCall(suspendOrReturnFunctionSymbol).apply {
                                putValueArgument(0, call)
                                putValueArgument(1, irCall(getContinuationSymbol))
                            }
                            val tmp = createTmpVariable(suspendOrReturnCall, irType = suspendOrReturnCall.type)
                            val coroutineSuspended = irCall(coroutineSuspendedGetterSymbol)
                            val condition = irEqeqeq(irGet(tmp), coroutineSuspended)
                            val yield = irCall(jsYieldFunctionSymbol).apply { putValueArgument(0, irGet(tmp)) }
                            +irIfThen(context.irBuiltIns.unitType, condition, irSet(tmp, yield))
                            +irImplicitCast(irGet(tmp), call.type)
                        }
                    }
                }
            }
        })
    }

    private fun removeReturnIfSuspendedCallAndSimplifyDelegatingCall(irFunction: IrFunction, delegatingCall: IrCall) {
        val returnValue = runIf(delegatingCall.isReturnIfSuspendedCall(context)) {
            delegatingCall.getValueArgument(0)
        } ?: delegatingCall

        val body = irFunction.body as IrBlockBody

        context.createIrBuilder(
            irFunction.symbol,
            startOffset = body.endOffset.previousOffset,
            endOffset = body.endOffset.previousOffset
        ).run {
            val statements = body.statements
            val lastStatement = statements.last()
            assert(lastStatement == delegatingCall || lastStatement is IrReturn) { "Unexpected statement $lastStatement" }

            val tempVar = scope.createTemporaryVariable(returnValue, irType = context.irBuiltIns.anyType)
            statements[statements.lastIndex] = tempVar
            statements.add(irReturn(irGet(tempVar)))
        }
    }
}