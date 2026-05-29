/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.compileSuspendAsJsGenerator
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.irFlag
import org.jetbrains.kotlin.ir.util.isSuspendFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.util.OperatorNameConventions

var IrFunction.shouldBeCompiledAsGenerator by irFlag(copyByDefault = true)

/**
 * Transforms suspend function into a GeneratorCoroutineImpl instance and ES2015 generator.
 */
class JsSuspendFunctionWithGeneratorsLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    private val jsYieldFunctionSymbol = context.symbols.jsYieldFunctionSymbol
    private val jsYieldStarFunctionSymbol = context.symbols.jsYieldStarFunctionSymbol
    private val lambdaRunFunctionSymbol = context.symbols.suspendLambdaRunFunctionSymbol

    override fun lower(irModule: IrModuleFragment) {
        if (!context.compileSuspendAsJsGenerator) return
        super.lower(irModule)
    }

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
     * - Call suspend lambdas via `suspendLambdaRun` intrinsic which allows to call both `async` and `suspend` lambdas in the same way
     *
     * Before:
     *  ```
     *  suspend fun foo(a: suspend () -> Unit) {
     *      println("Hello")
     *      suspendHere()
     *      println("World")
     *      a()
     *      return 1
     *  }
     *  ```
     *
     * After:
     *  ```
     *  [shouldBeCompiledAsGenerator = true]
     *  suspend fun foo(a: suspend () -> Unit) {
     *      println("Hello")
     *      jsYieldStar(suspendHere())
     *      println("World")
     *      jsYieldStar(suspendLambdaRun(a()))
     *      return 1
     *  }
     *  ```
     */
    private fun convertSuspendFunctionToGenerator(function: IrSimpleFunction, functionBody: IrBody) {
        function.shouldBeCompiledAsGenerator = true
        functionBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)
                val callee = expression.symbol.owner
                return when {
                    !callee.isSuspend -> expression
                    else -> {
                        val yieldStarArgument = if (callee.isSuspendLambdaInvoke()) {
                            JsIrBuilder.buildCall(lambdaRunFunctionSymbol, expression.type, listOf(expression.type))
                                .apply { arguments[0] = expression }
                        } else expression

                        JsIrBuilder.buildCall(jsYieldStarFunctionSymbol, expression.type, listOf(expression.type))
                            .apply { arguments[0] = yieldStarArgument }
                    }
                }
            }
        })
    }

    private fun IrSimpleFunction.isSuspendLambdaInvoke(): Boolean {
        val dispatchReceiver = dispatchReceiverParameter ?: return false
        return name == OperatorNameConventions.INVOKE && dispatchReceiver.type.isSuspendFunction()
    }
}
