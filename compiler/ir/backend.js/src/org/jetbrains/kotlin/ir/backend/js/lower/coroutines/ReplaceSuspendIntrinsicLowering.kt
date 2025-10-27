/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.ir.move
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.compileSuspendAsJsGenerator
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.SpecialNames

/**
 * Replaces suspend intrinsic for generator-based coroutines.
 */
class ReplaceSuspendIntrinsicLowering(private val context: JsIrBackendContext) : BodyLoweringPass {
    private val jsYield =
        context.symbols.jsYieldFunctionSymbol
    private val returnIfSuspendedNonGeneratorVersion =
        context.symbols.returnIfSuspendedNonGeneratorVersion
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
            override fun visitReturnableBlock(expression: IrReturnableBlock): IrExpression {
                val inlinedBlock = expression.statements.singleOrNull() as? IrInlinedFunctionBlock

                if (inlinedBlock?.inlinedFunctionSymbol != returnIfSuspendedNonGeneratorVersion) {
                    return super.visitReturnableBlock(expression)
                }

                return JsIrBuilder.buildCall(
                    jsYield,
                    expression.type,
                    listOf(expression.type),
                ).apply { arguments[0] = super.visitFunctionExpression(expression.wrapInAnonymousFunction(inlinedBlock, container)) }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                when (val symbol = expression.symbol) {
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

    private fun IrReturnableBlock.wrapInAnonymousFunction(
        inlinedBlock: IrInlinedFunctionBlock,
        container: IrDeclaration,
    ): IrFunctionExpression {
        val returnableBlockSymbol = symbol
        val wrapperFunction = context.irFactory.buildFun {
            name = SpecialNames.NO_NAME_PROVIDED
            visibility = DescriptorVisibilities.LOCAL
            isSuspend = false
            returnType = inlinedBlock.type
        }.also {
            it.parent = container as IrDeclarationParent
            it.body = IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
                .apply { statements.addAll(inlinedBlock.statements) }
                .move(container, returnableBlockSymbol, it, it.symbol, emptyMap())
        }

        return JsIrBuilder.buildFunctionExpression(
            context.symbols.dynamicType,
            wrapperFunction
        )
    }
}
