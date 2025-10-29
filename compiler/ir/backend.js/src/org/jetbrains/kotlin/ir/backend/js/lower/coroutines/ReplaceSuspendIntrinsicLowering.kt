/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.compileSuspendAsJsGenerator
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.SpecialNames

val YIELDED_WRAPPER_FUNCTION by IrDeclarationOriginImpl.Synthetic

/**
 * Replaces suspend intrinsic for generator-based coroutines.
 */
class ReplaceSuspendIntrinsicLowering(private val context: JsIrBackendContext) : BodyLoweringPass {
    private val jsYield =
        context.symbols.jsYieldFunctionSymbol
    private val returnIfSuspended =
        context.symbols.returnIfSuspended
    private val valueParamSizeToItsCreateCoroutineUnintercepted =
        context.symbols.createCoroutineUninterceptedGeneratorVersion.groupPerValueParamSize()
    private val valueParamSizeToItsStartCoroutineUninterceptedOrReturnGeneratorVersion =
        context.symbols.startCoroutineUninterceptedOrReturnGeneratorVersion.groupPerValueParamSize()

    private val IrSimpleFunctionSymbol.regularParamCount: Int
        get() = owner.parameters.count { it.kind == IrParameterKind.Regular }

    private fun Set<IrSimpleFunctionSymbol>.groupPerValueParamSize(): Map<Int, IrSimpleFunctionSymbol> {
        return associateBy { it.regularParamCount }
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (!context.compileSuspendAsJsGenerator) return
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            private val containerFunctionStack = mutableListOf(container)

            override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
                containerFunctionStack.add(declaration)
                return super.visitSimpleFunction(declaration).also { containerFunctionStack.removeLast() }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                when (val symbol = expression.symbol) {
                    returnIfSuspended -> {
                        val returnedValue = expression.arguments.singleOrNull()
                            ?: compilationException("Expected exactly one argument for returnIfSuspended function call", expression)

                        return super.visitExpression(returnedValue)
                            .toGeneratorSuspensionExpression(containerFunctionStack.last())
                    }
                    in context.symbols.createCoroutineUnintercepted -> {
                        expression.symbol = valueParamSizeToItsCreateCoroutineUnintercepted.getValue(symbol.regularParamCount)
                    }
                    in context.symbols.startCoroutineUninterceptedOrReturnNonGeneratorVersion -> {
                        expression.symbol =
                            valueParamSizeToItsStartCoroutineUninterceptedOrReturnGeneratorVersion.getValue(symbol.regularParamCount)
                    }
                }
                return super.visitCall(expression)
            }
        })
    }

    private fun IrExpression.toGeneratorSuspensionExpression(container: IrDeclaration): IrExpression {
        val wrapperFunction = context.irFactory.buildFun {
            name = SpecialNames.NO_NAME_PROVIDED
            visibility = DescriptorVisibilities.LOCAL
            isSuspend = false
            returnType = this@toGeneratorSuspensionExpression.type
            origin = YIELDED_WRAPPER_FUNCTION
        }.also {
            it.parent = container as IrDeclarationParent
            it.body = with(context.createIrBuilder(it.symbol)) {
                irBlockBody {
                    +irReturn(this@toGeneratorSuspensionExpression)
                }
            }
        }

        return JsIrBuilder.buildCall(jsYield).apply {
            arguments[0] = JsIrBuilder.buildFunctionExpression(context.dynamicType, wrapperFunction)
        }
    }
}
