/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.getVoid
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.isNullableString
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrTransformer

/**
 * Links [kotlin.Throwable] and JavaScript `Error` together to provide proper interop between language and platform exceptions.
 */
class ThrowableLowering(val context: JsIrBackendContext, val extendThrowableFunction: IrSimpleFunctionSymbol) : BodyLoweringPass {
    private val throwableConstructors = context.throwableConstructors
    private val newThrowableFunction = context.newThrowableSymbol

    private fun undefinedValue(): IrExpression = context.getVoid()

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
        when (arguments.size) {
            0 -> ThrowableArguments(undefinedValue(), undefinedValue())
            2 -> ThrowableArguments(
                message = arguments[0]!!,
                cause = arguments[1]!!
            )
            else -> {
                val arg = arguments[0]!!
                val parameter = symbol.owner.parameters[0]
                when {
                    parameter.type.isNullableString() -> ThrowableArguments(message = arg, cause = undefinedValue())
                    else -> {
                        assert(parameter.type.makeNotNull().isThrowable())
                        ThrowableArguments(message = undefinedValue(), cause = arg)
                    }
                }
            }
        }

    inner class Transformer : IrTransformer<IrDeclarationParent>() {
        private val anyConstructor = context.irBuiltIns.anyClass.constructors.first()

        override fun visitClass(declaration: IrClass, data: IrDeclarationParent) = super.visitClass(declaration, declaration)

        override fun visitConstructorCall(expression: IrConstructorCall, data: IrDeclarationParent): IrExpression {
            expression.transformChildren(this, data)
            if (expression.symbol !in throwableConstructors) return expression

            val (messageArg, causeArg) = expression.extractThrowableArguments()

            return expression.run {
                IrCallImpl(
                    startOffset, endOffset, type, newThrowableFunction,
                    typeArgumentsCount = 0
                ).also {
                    it.arguments[0] = messageArg
                    it.arguments[1] = causeArg
                }
            }
        }

        override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: IrDeclarationParent): IrExpression {
            expression.transformChildren(this, data)
            if (expression.symbol !in throwableConstructors) return expression

            val (messageArg, causeArg) = expression.extractThrowableArguments()

            val klass = data as IrClass
            val thisReceiver = IrGetValueImpl(expression.startOffset, expression.endOffset, klass.thisReceiver!!.symbol)

            val expressionReplacement = expression.run {
                IrCallImpl(
                    startOffset, endOffset, type, extendThrowableFunction,
                    typeArgumentsCount = 0
                ).apply {
                    arguments[0] = thisReceiver
                    arguments[1] = messageArg
                    arguments[2] = causeArg
                }
            }

            return if (!context.es6mode) {
                expressionReplacement
            } else {
                JsIrBuilder.buildComposite(
                    context.irBuiltIns.unitType,
                    listOf(
                        IrDelegatingConstructorCallImpl(
                            expression.startOffset,
                            expression.endOffset,
                            context.irBuiltIns.anyType,
                            anyConstructor,
                            0,
                        ),
                        expressionReplacement
                    )
                )
            }
        }
    }
}