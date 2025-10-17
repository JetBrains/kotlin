/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.previousOffset
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

/**
 * Transforms suspend function into a GeneratorCoroutineImpl instance and ES2015 generator.
 */
class JsSuspendFunctionWithGeneratorsLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    private val jsYieldFunctionSymbol = context.symbols.jsYieldFunctionSymbol
    private val jsYieldStarFunctionSymbol = context.symbols.jsYieldStarFunctionSymbol

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrSimpleFunction && declaration.isSuspend) {
            transformSuspendFunction(declaration)
        }
        return null
    }

    private fun transformSuspendFunction(function: IrSimpleFunction) {
        val body = function.body ?: return
        return when (
            val functionKind = getSuspendFunctionKind(
                context,
                function,
                body,
                includeSuspendLambda = false,
                suspensionIntrinsic = jsYieldFunctionSymbol
            )
        ) {
            is SuspendFunctionKind.NO_SUSPEND_CALLS -> {
                function.addJsGeneratorAnnotation()
            }
            is SuspendFunctionKind.DELEGATING -> {
                removeReturnIfSuspendedCallAndSimplifyDelegatingCall(function, functionKind.delegatingCall)
            }
            is SuspendFunctionKind.NEEDS_STATE_MACHINE -> {
                convertSuspendFunctionToGenerator(function, body)
            }
        }
    }

    private fun IrSimpleFunction.addJsGeneratorAnnotation() {
        annotations = annotations memoryOptimizedPlus JsIrBuilder.buildConstructorCall(
            context.symbols.jsGeneratorAnnotationSymbol.owner.primaryConstructor!!.symbol
        )
    }

    private fun convertSuspendFunctionToGenerator(function: IrSimpleFunction, functionBody: IrBody) {
        function.addJsGeneratorAnnotation()
        functionBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val call = super.visitCall(expression)
                return if (call !is IrCall || !call.symbol.owner.isSuspend) {
                    call
                } else {
                    JsIrBuilder.buildCall(
                        jsYieldStarFunctionSymbol,
                        call.type,
                        listOf(call.type)
                    ).apply { arguments[0] = call }
                }
            }
        })
    }

    private fun removeReturnIfSuspendedCallAndSimplifyDelegatingCall(irFunction: IrFunction, delegatingCall: IrCall) {
        val returnValue = runIf(delegatingCall.isReturnIfSuspendedCall(context)) {
            delegatingCall.arguments[0]
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