/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.ir.isOverridableOrOverrides
import org.jetbrains.kotlin.backend.common.lower.DefaultArgumentStubGenerator
import org.jetbrains.kotlin.backend.common.lower.DEFAULT_DISPATCH_CALL
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class JsDefaultArgumentStubGenerator(override val context: JsIrBackendContext) : DefaultArgumentStubGenerator(context, true, true, false) {

    override fun needSpecialDispatch(irFunction: IrSimpleFunction) = irFunction.isOverridableOrOverrides

    override fun IrBlockBodyBuilder.generateHandleCall(
        handlerDeclaration: IrValueParameter,
        oldIrFunction: IrFunction,
        newIrFunction: IrFunction,
        params: MutableList<IrValueDeclaration>
    ): IrExpression {
        val paramCount = oldIrFunction.valueParameters.size
        val invokeFunctionN = resolveInvoke(paramCount)
        // NOTE: currently we do not have a syntax to perform super extension call
        // but in case we have such functionality in the future the logic bellow should be fixed
        return irCall(invokeFunctionN, IrStatementOrigin.INVOKE).apply {
            dispatchReceiver = irImplicitCast(irGet(handlerDeclaration), invokeFunctionN.dispatchReceiverParameter!!.type)
            assert(newIrFunction.extensionReceiverParameter == null)
            params.forEachIndexed { i, variable -> putValueArgument(i, irGet(variable)) }
        }
    }

    private fun resolveInvoke(paramCount: Int): IrSimpleFunction {
        assert(paramCount > 0)
        val functionKlass = context.ir.symbols.functionN(paramCount).owner
        return functionKlass.declarations.filterIsInstance<IrSimpleFunction>().first { it.name == Name.identifier("invoke") }
    }
}

val BIND_CALL = object : IrStatementOriginImpl("BIND_CALL") {}

class JsDefaultCallbackGenerator(val context: JsIrBackendContext): BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)
                if (expression.origin != DEFAULT_DISPATCH_CALL || expression.superQualifierSymbol == null) return expression

                val binding = buildBoundSuperCall(expression)

                expression.putValueArgument(expression.valueArgumentsCount - 1, binding)

                return expression
            }
        })
    }

    private fun buildBoundSuperCall(irCall: IrCall): IrExpression {

        val originalFunction = context.mapping.defaultArgumentsOriginalFunction[irCall.symbol.owner]!!

        val reference = irCall.run {
            IrFunctionReferenceImpl(
                startOffset,
                endOffset,
                context.irBuiltIns.anyType,
                originalFunction.symbol,
                typeArgumentsCount = 0,
                valueArgumentsCount = originalFunction.valueParameters.size,
                reflectionTarget = originalFunction.symbol,
                origin = BIND_CALL
            )
        }

        return irCall.run {
            IrCallImpl(
                startOffset,
                endOffset,
                context.irBuiltIns.anyType,
                context.intrinsics.jsBind.symbol,
                valueArgumentsCount = 2,
                typeArgumentsCount = 0,
                origin = BIND_CALL,
                superQualifierSymbol = superQualifierSymbol
            )
        }.apply {
            putValueArgument(0, irCall.dispatchReceiver?.deepCopyWithSymbols())
            putValueArgument(1, reference)
        }
    }

}