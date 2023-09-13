/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.previousOffset
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.addToStdlib.runIf

object LOWERED_GENERATOR_FUNCTION : IrDeclarationOriginImpl("LOWERED_GENERATOR_FUNCTION")

class JsSuspendFunctionWithGeneratorsLowering(private val context: JsIrBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container is IrSimpleFunction && container.isSuspend) {
            transformSuspendFunction(container, irBody)
        }
    }

    private fun transformSuspendFunction(function: IrSimpleFunction, body: IrBody): IrFunction {
        return function.apply {
            when (val functionKind = getSuspendFunctionKind(context, this, body)) {
                is SuspendFunctionKind.NO_SUSPEND_CALLS -> {}
                is SuspendFunctionKind.DELEGATING -> {
                    removeReturnIfSuspendedCallAndSimplifyDelegatingCall(this, functionKind.delegatingCall)
                }
                is SuspendFunctionKind.NEEDS_STATE_MACHINE -> {
                    origin = LOWERED_GENERATOR_FUNCTION
                    putYieldIntrinsicCallInAllSuspensionPoints(body)
                }
            }
        }
    }

    private fun putYieldIntrinsicCallInAllSuspensionPoints(body: IrBody) {
        body.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                return if (expression.symbol.owner.isSuspend) {
                    JsIrBuilder.buildCall(
                        context.intrinsics.jsYieldFunctionSymbol,
                        expression.type,
                        listOf(expression.type)
                    ).apply { putValueArgument(0, super.visitCall(expression)) }
                } else {
                    super.visitCall(expression)
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