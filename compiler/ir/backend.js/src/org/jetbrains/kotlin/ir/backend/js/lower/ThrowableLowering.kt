/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.isNullableString
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer


class ThrowableLowering(
    val context: JsIrBackendContext,
    val extendThrowableFunction: IrSimpleFunctionSymbol
) : BodyLoweringPass {
    private val nothingNType get() = context.irBuiltIns.nothingNType

    private val throwableConstructors = context.throwableConstructors
    private val newThrowableFunction = context.newThrowableSymbol

    fun nullValue(): IrExpression = IrConstImpl.constNull(UNDEFINED_OFFSET, UNDEFINED_OFFSET, nothingNType)

    data class ThrowableArguments(
        val message: IrExpression,
        val cause: IrExpression
    )

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        container.parentClassOrNull.let { enclosingClass ->
            irBody.transformChildren(Transformer(), enclosingClass ?: container.file)
        }
    }

    private fun IrFunctionAccessExpression.extractThrowableArguments(): ThrowableArguments =
        when (valueArgumentsCount) {
            0 -> ThrowableArguments(nullValue(), nullValue())
            2 -> ThrowableArguments(
                message = getValueArgument(0)!!,
                cause = getValueArgument(1)!!
            )
            else -> {
                val arg = getValueArgument(0)!!
                val parameter = symbol.owner.valueParameters[0]
                when {
                    parameter.type.isNullableString() -> ThrowableArguments(message = arg, cause = nullValue())
                    else -> {
                        assert(parameter.type.makeNotNull().isThrowable())
                        ThrowableArguments(message = nullValue(), cause = arg)
                    }
                }
            }
        }

    inner class Transformer : IrElementTransformer<IrDeclarationParent> {
        override fun visitClass(declaration: IrClass, data: IrDeclarationParent) = super.visitClass(declaration, declaration)

        override fun visitConstructorCall(expression: IrConstructorCall, data: IrDeclarationParent): IrExpression {
            expression.transformChildren(this, data)
            if (expression.symbol !in throwableConstructors) return expression

            val (messageArg, causeArg) = expression.extractThrowableArguments()

            return expression.run {
                IrCallImpl(
                    startOffset, endOffset, type, newThrowableFunction,
                    valueArgumentsCount = 2,
                    typeArgumentsCount = 0
                ).also {
                    it.putValueArgument(0, messageArg)
                    it.putValueArgument(1, causeArg)
                }
            }
        }

        override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: IrDeclarationParent): IrExpression {
            expression.transformChildren(this, data)
            if (expression.symbol !in throwableConstructors) return expression

            val (messageArg, causeArg) = expression.extractThrowableArguments()

            val klass = data as IrClass
            val thisReceiver = IrGetValueImpl(expression.startOffset, expression.endOffset, klass.thisReceiver!!.symbol)

            return expression.run {
                IrCallImpl(
                    startOffset, endOffset, type, extendThrowableFunction,
                    valueArgumentsCount = 3,
                    typeArgumentsCount = 0
                ).also {
                    it.putValueArgument(0, thisReceiver)
                    it.putValueArgument(1, messageArg)
                    it.putValueArgument(2, causeArg)
                }
            }
        }
    }
}