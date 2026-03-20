/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.compileSuspendAsJsGenerator
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

val YIELDED_WRAPPER_FUNCTION by IrDeclarationOriginImpl.Synthetic

/**
 * Replaces suspend intrinsic for generator-based coroutines.
 */
class ReplaceSuspendIntrinsicLowering(private val context: JsIrBackendContext) : BodyLoweringPass {
    private val returnIfSuspended =
        context.symbols.returnIfSuspended
    private val safeGeneratorContinuationFor =
        context.symbols.safeGeneratorContinuationFor
    private val valueParamSizeToItsCreateCoroutineUnintercepted =
        context.symbols.createCoroutineUninterceptedGeneratorVersion.groupPerValueParamSize()
    private val valueParamSizeToItsStartCoroutineUninterceptedOrReturnGeneratorVersion =
        context.symbols.startCoroutineUninterceptedOrReturnGeneratorVersion.groupPerValueParamSize()

    private val compileSuspendAsJsGenerator = context.compileSuspendAsJsGenerator

    private val IrSimpleFunctionSymbol.regularParamCount: Int
        get() = owner.parameters.count { it.kind == IrParameterKind.Regular }

    private fun Set<IrSimpleFunctionSymbol>.groupPerValueParamSize(): Map<Int, IrSimpleFunctionSymbol> {
        return associateBy { it.regularParamCount }
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            private val containerFunctionStack = mutableListOf(container)

            override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
                containerFunctionStack.add(declaration)
                return super.visitSimpleFunction(declaration).also { containerFunctionStack.removeLast() }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                when (val symbol = expression.symbol) {
                    in context.symbols.createCoroutineUnintercepted if compileSuspendAsJsGenerator -> {
                        expression.symbol = valueParamSizeToItsCreateCoroutineUnintercepted.getValue(symbol.regularParamCount)
                    }
                    in context.symbols.startCoroutineUninterceptedOrReturnNonGeneratorVersion if compileSuspendAsJsGenerator -> {
                        expression.symbol =
                            valueParamSizeToItsStartCoroutineUninterceptedOrReturnGeneratorVersion.getValue(symbol.regularParamCount)
                    }
                    safeGeneratorContinuationFor if !compileSuspendAsJsGenerator -> {
                        return super.visitExpression(
                            expression.arguments.firstOrNull() ?: compilationException(
                                "Expected exactly at least one argument for safeGeneratorContinuationFor function call",
                                expression
                            )
                        )
                    }
                    returnIfSuspended if !compileSuspendAsJsGenerator -> {
                        return super.visitExpression(
                            expression.arguments.singleOrNull()
                                ?: compilationException("Expected exactly one argument for returnIfSuspended function call", expression)
                        )
                    }
                }

                return super.visitCall(expression)
            }
        })
    }
}
