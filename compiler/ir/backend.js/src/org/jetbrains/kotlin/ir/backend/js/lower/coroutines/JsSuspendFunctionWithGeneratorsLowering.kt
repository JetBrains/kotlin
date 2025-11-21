/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.irFlag
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

var IrFunction.shouldBeCompiledAsGenerator by irFlag(copyByDefault = true)

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
        return when (getSuspendFunctionKind(
            context,
            function,
            body,
            includeSuspendLambda = false,
            suspensionIntrinsic = jsYieldFunctionSymbol
        )) {
            is SuspendFunctionKind.NO_SUSPEND_CALLS -> {
                function.shouldBeCompiledAsGenerator = true
            }
            is SuspendFunctionKind.DELEGATING, is SuspendFunctionKind.NEEDS_STATE_MACHINE -> {
                convertSuspendFunctionToGenerator(function, body)
            }
        }
    }

    /**
     * The lowering transforms the suspend function in the way it could be generated as a generator:
     * - Add [shouldBeCompiledAsGenerator] flag to mark a function as a generator for the codegen
     * - At each suspension point we put an intrinsic for `yield*` statement, so that the generator could be resumed from the suspension point
     *
     * Before:
     *  ```
     *  suspend fun foo() {
     *      println("Hello")
     *      suspendHere()
     *      println("World")
     *      return 1
     *  }
     *  ```
     *
     * After:
     *  ```
     *  [shouldBeCompiledAsGenerator = true]
     *  suspend fun foo() {
     *      println("Hello")
     *      jsYieldStar(suspendHere())
     *      println("World")
     *      return 1
     *  }
     *  ```
     */
    private fun convertSuspendFunctionToGenerator(function: IrSimpleFunction, functionBody: IrBody) {
        function.shouldBeCompiledAsGenerator = true
        functionBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val call = super.visitCall(expression)
                return if (call !is IrCall || !call.symbol.owner.isSuspend) {
                    call
                } else {
                    JsIrBuilder.buildCall(jsYieldStarFunctionSymbol, call.type, listOf(call.type))
                        .apply { arguments[0] = call }
                }
            }
        })
    }
}